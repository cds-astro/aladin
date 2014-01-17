   
package cds.image;

/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */


import java.io.IOException;
import java.io.InputStream;

/**
 * An input stream that decompresses from the BZip2 format (without the file
 * header chars) to be read as any other stream.
 *
 * @author <a href="mailto:keiron@aftexsw.com">Keiron Liddle</a>
 * A few adaptation has been done by P.Fernique [CDS] for integrating it in Aladin.
 */
public class Bzip2
    extends InputStream
    implements BZip2Constants
{
    private static final int START_BLOCK_STATE = 1;
    private static final int RAND_PART_A_STATE = 2;
    private static final int RAND_PART_B_STATE = 3;
    private static final int RAND_PART_C_STATE = 4;
    private static final int NO_RAND_PART_A_STATE = 5;
    private static final int NO_RAND_PART_B_STATE = 6;
    private static final int NO_RAND_PART_C_STATE = 7;

    private CRC m_crc = new CRC();
    private boolean[] m_inUse = new boolean[ 256 ];
    private char[] m_seqToUnseq = new char[ 256 ];
    private char[] m_unseqToSeq = new char[ 256 ];
    private char[] m_selector = new char[ MAX_SELECTORS ];
    private char[] m_selectorMtf = new char[ MAX_SELECTORS ];

    /*
     * freq table collected to save a pass over the data
     * during decompression.
     */
    private int[] m_unzftab = new int[ 256 ];

    private int[][] m_limit = new int[ N_GROUPS ][ MAX_ALPHA_SIZE ];
    private int[][] m_base = new int[ N_GROUPS ][ MAX_ALPHA_SIZE ];
    private int[][] m_perm = new int[ N_GROUPS ][ MAX_ALPHA_SIZE ];
    private int[] m_minLens = new int[ N_GROUPS ];

    private boolean m_streamEnd;
    private int m_currentChar = -1;

    private int m_currentState = START_BLOCK_STATE;
    private int m_rNToGo;
    private int m_rTPos;
    private int m_tPos;

    private int i2;
    private int count;
    private int chPrev;
    private int ch2;
    private int j2;
    private char z;

    private boolean m_blockRandomised;

    /*
     * always: in the range 0 .. 9.
     * The current block size is 100000 * this number.
     */
    private int m_blockSize100k;
    private int m_bsBuff;
    private int m_bsLive;

    private InputStream m_input;

    private int m_computedBlockCRC;
    private int m_computedCombinedCRC;

    /*
     * index of the last char in the block, so
     * the block size == last + 1.
     */
    private int m_last;
    private char[] m_ll8;
    private int m_nInUse;

    /*
     * index in zptr[] of original string after sorting.
     */
    private int m_origPtr;

    private int m_storedBlockCRC;
    private int m_storedCombinedCRC;
    private int[] m_tt;

    public Bzip2( final InputStream input )
    {
        bsSetStream( input );
        initialize();
        initBlock();
        setupBlock();
    }

    private static void badBlockHeader()
    {
        cadvise();
    }

    private static void blockOverrun()
    {
        cadvise();
    }

    private static void cadvise()
    {
//        System.out.println( "CRC Error" );
        //throw new CCoruptionError();
    }

    private static void compressedStreamEOF()
    {
        cadvise();
    }

    private static void crcError()
    {
        cadvise();
    }

    public int read()
    {
        if( m_streamEnd )
        {
            return -1;
        }
        else
        {
            int retChar = m_currentChar;
            switch( m_currentState )
            {
                case START_BLOCK_STATE:
                    break;
                case RAND_PART_A_STATE:
                    break;
                case RAND_PART_B_STATE:
                    setupRandPartB();
                    break;
                case RAND_PART_C_STATE:
                    setupRandPartC();
                    break;
                case NO_RAND_PART_A_STATE:
                    break;
                case NO_RAND_PART_B_STATE:
                    setupNoRandPartB();
                    break;
                case NO_RAND_PART_C_STATE:
                    setupNoRandPartC();
                    break;
                default:
                    break;
            }
            return retChar;
        }
    }

    private void setDecompressStructureSizes( int newSize100k )
    {
        if( !( 0 <= newSize100k && newSize100k <= 9 && 0 <= m_blockSize100k
            && m_blockSize100k <= 9 ) )
        {
            // throw new IOException("Invalid block size");
        }

        m_blockSize100k = newSize100k;

        if( newSize100k == 0 )
        {
            return;
        }

        int n = BASE_BLOCK_SIZE * newSize100k;
        m_ll8 = new char[ n ];
        m_tt = new int[ n ];
    }

    private void setupBlock()
    {
        int[] cftab = new int[ 257 ];
        char ch;

        cftab[ 0 ] = 0;
        for( int i = 1; i <= 256; i++ )
        {
            cftab[ i ] = m_unzftab[ i - 1 ];
        }
        for( int i = 1; i <= 256; i++ )
        {
            cftab[ i ] += cftab[ i - 1 ];
        }

        for( int i = 0; i <= m_last; i++ )
        {
            ch = m_ll8[ i ];
            m_tt[ cftab[ ch ] ] = i;
            cftab[ ch ]++;
        }
        cftab = null;

        m_tPos = m_tt[ m_origPtr ];

        count = 0;
        i2 = 0;
        ch2 = 256;
        /*
         * not a char and not EOF
         */
        if( m_blockRandomised )
        {
            m_rNToGo = 0;
            m_rTPos = 0;
            setupRandPartA();
        }
        else
        {
            setupNoRandPartA();
        }
    }

    private void setupNoRandPartA()
    {
        if( i2 <= m_last )
        {
            chPrev = ch2;
            ch2 = m_ll8[ m_tPos ];
            m_tPos = m_tt[ m_tPos ];
            i2++;

            m_currentChar = ch2;
            m_currentState = NO_RAND_PART_B_STATE;
            m_crc.updateCRC( ch2 );
        }
        else
        {
            endBlock();
            initBlock();
            setupBlock();
        }
    }

    private void setupNoRandPartB()
    {
        if( ch2 != chPrev )
        {
            m_currentState = NO_RAND_PART_A_STATE;
            count = 1;
            setupNoRandPartA();
        }
        else
        {
            count++;
            if( count >= 4 )
            {
                z = m_ll8[ m_tPos ];
                m_tPos = m_tt[ m_tPos ];
                m_currentState = NO_RAND_PART_C_STATE;
                j2 = 0;
                setupNoRandPartC();
            }
            else
            {
                m_currentState = NO_RAND_PART_A_STATE;
                setupNoRandPartA();
            }
        }
    }

    private void setupNoRandPartC()
    {
        if( j2 < z )
        {
            m_currentChar = ch2;
            m_crc.updateCRC( ch2 );
            j2++;
        }
        else
        {
            m_currentState = NO_RAND_PART_A_STATE;
            i2++;
            count = 0;
            setupNoRandPartA();
        }
    }

    private void setupRandPartA()
    {
        if( i2 <= m_last )
        {
            chPrev = ch2;
            ch2 = m_ll8[ m_tPos ];
            m_tPos = m_tt[ m_tPos ];
            if( m_rNToGo == 0 )
            {
                m_rNToGo = RAND_NUMS[ m_rTPos ];
                m_rTPos++;
                if( m_rTPos == 512 )
                {
                    m_rTPos = 0;
                }
            }
            m_rNToGo--;
            ch2 ^= ( ( m_rNToGo == 1 ) ? 1 : 0 );
            i2++;

            m_currentChar = ch2;
            m_currentState = RAND_PART_B_STATE;
            m_crc.updateCRC( ch2 );
        }
        else
        {
            endBlock();
            initBlock();
            setupBlock();
        }
    }

    private void setupRandPartB()
    {
        if( ch2 != chPrev )
        {
            m_currentState = RAND_PART_A_STATE;
            count = 1;
            setupRandPartA();
        }
        else
        {
            count++;
            if( count >= 4 )
            {
                z = m_ll8[ m_tPos ];
                m_tPos = m_tt[ m_tPos ];
                if( m_rNToGo == 0 )
                {
                    m_rNToGo = RAND_NUMS[ m_rTPos ];
                    m_rTPos++;
                    if( m_rTPos == 512 )
                    {
                        m_rTPos = 0;
                    }
                }
                m_rNToGo--;
                z ^= ( ( m_rNToGo == 1 ) ? 1 : 0 );
                j2 = 0;
                m_currentState = RAND_PART_C_STATE;
                setupRandPartC();
            }
            else
            {
                m_currentState = RAND_PART_A_STATE;
                setupRandPartA();
            }
        }
    }

    private void setupRandPartC()
    {
        if( j2 < z )
        {
            m_currentChar = ch2;
            m_crc.updateCRC( ch2 );
            j2++;
        }
        else
        {
            m_currentState = RAND_PART_A_STATE;
            i2++;
            count = 0;
            setupRandPartA();
        }
    }

    private void getAndMoveToFrontDecode()
    {
        int nextSym;

        int limitLast = BASE_BLOCK_SIZE * m_blockSize100k;
        m_origPtr = readVariableSizedInt( 24 );

        recvDecodingTables();
        int EOB = m_nInUse + 1;
        int groupNo = -1;
        int groupPos = 0;

        /*
         * Setting up the unzftab entries here is not strictly
         * necessary, but it does save having to do it later
         * in a separate pass, and so saves a block's worth of
         * cache misses.
         */
        for( int i = 0; i <= 255; i++ )
        {
            m_unzftab[ i ] = 0;
        }

        final char[] yy = new char[ 256 ];
        for( int i = 0; i <= 255; i++ )
        {
            yy[ i ] = (char)i;
        }

        m_last = -1;
        int zt;
        int zn;
        int zvec;
        int zj;
        groupNo++;
        groupPos = G_SIZE - 1;

        zt = m_selector[ groupNo ];
        zn = m_minLens[ zt ];
        zvec = bsR( zn );
        while( zvec > m_limit[ zt ][ zn ] )
        {
            zn++;

            while( m_bsLive < 1 )
            {
                int zzi;
                try
                {
                    zzi = m_input.read();
                }
                catch( IOException e )
                {
                    compressedStreamEOF();
          break;
                }
                if( zzi == -1 )
                {
                    compressedStreamEOF();
          break;
                }
                m_bsBuff = ( m_bsBuff << 8 ) | ( zzi & 0xff );
                m_bsLive += 8;
            }

            zj = ( m_bsBuff >> ( m_bsLive - 1 ) ) & 1;
            m_bsLive--;

            zvec = ( zvec << 1 ) | zj;
        }
        nextSym = m_perm[ zt ][ zvec - m_base[ zt ][ zn ] ];

        while( true )
        {
            if( nextSym == EOB )
            {
                break;
            }

            if( nextSym == RUNA || nextSym == RUNB )
            {
                char ch;
                int s = -1;
                int N = 1;
                do
                {
                    if( nextSym == RUNA )
                    {
                        s = s + ( 0 + 1 ) * N;
                    }
                    else// if( nextSym == RUNB )
                    {
                        s = s + ( 1 + 1 ) * N;
                    }
                    N = N * 2;

                    if( groupPos == 0 )
                    {
                        groupNo++;
                        groupPos = G_SIZE;
                    }
                    groupPos--;
                    zt = m_selector[ groupNo ];
                    zn = m_minLens[ zt ];
                    zvec = bsR( zn );
                    while( zvec > m_limit[ zt ][ zn ] )
                    {
                        zn++;

                        while( m_bsLive < 1 )
                        {
                            int zzi;
                            char thech = 0;
                            try
                            {
                                thech = (char)m_input.read();
                            }
                            catch( IOException e )
                            {
                                compressedStreamEOF();
                            }
                            if( thech == -1 )
                            {
                                compressedStreamEOF();
                            }
                            zzi = thech;
                            m_bsBuff = ( m_bsBuff << 8 ) | ( zzi & 0xff );
                            m_bsLive += 8;
                        }

                        zj = ( m_bsBuff >> ( m_bsLive - 1 ) ) & 1;
                        m_bsLive--;
                        zvec = ( zvec << 1 ) | zj;
                    }

                    nextSym = m_perm[ zt ][ zvec - m_base[ zt ][ zn ] ];

                } while( nextSym == RUNA || nextSym == RUNB );

                s++;
                ch = m_seqToUnseq[ yy[ 0 ] ];
                m_unzftab[ ch ] += s;

                while( s > 0 )
                {
                    m_last++;
                    m_ll8[ m_last ] = ch;
                    s--;
                }

                if( m_last >= limitLast )
                {
                    blockOverrun();
                }
                continue;
            }
            else
            {
                char tmp;
                m_last++;
                if( m_last >= limitLast )
                {
                    blockOverrun();
                }

                tmp = yy[ nextSym - 1 ];
                m_unzftab[ m_seqToUnseq[ tmp ] ]++;
                m_ll8[ m_last ] = m_seqToUnseq[ tmp ];

                /*
                 * This loop is hammered during decompression,
                 * hence the unrolling.
                 * for (j = nextSym-1; j > 0; j--) yy[j] = yy[j-1];
                 */
                int j = nextSym - 1;
                for( ; j > 3; j -= 4 )
                {
                    yy[ j ] = yy[ j - 1 ];
                    yy[ j - 1 ] = yy[ j - 2 ];
                    yy[ j - 2 ] = yy[ j - 3 ];
                    yy[ j - 3 ] = yy[ j - 4 ];
                }
                for( ; j > 0; j-- )
                {
                    yy[ j ] = yy[ j - 1 ];
                }

                yy[ 0 ] = tmp;

                if( groupPos == 0 )
                {
                    groupNo++;
                    groupPos = G_SIZE;
                }
                groupPos--;
                zt = m_selector[ groupNo ];
                zn = m_minLens[ zt ];
                zvec = bsR( zn );
                while( zvec > m_limit[ zt ][ zn ] )
                {
                    zn++;

                    while( m_bsLive < 1 )
                    {
                        char ch = 0;
                        try
                        {
                            ch = (char)m_input.read();
                        }
                        catch( IOException e )
                        {
                            compressedStreamEOF();
                        }

                        m_bsBuff = ( m_bsBuff << 8 ) | ( ch & 0xff );
                        m_bsLive += 8;
                    }

                    zj = ( m_bsBuff >> ( m_bsLive - 1 ) ) & 1;
                    m_bsLive--;

                    zvec = ( zvec << 1 ) | zj;
                }
                nextSym = m_perm[ zt ][ zvec - m_base[ zt ][ zn ] ];

                continue;
            }
        }
    }

    private void bsFinishedWithStream()
    {
        m_input = null;
    }

    private int readVariableSizedInt( final int numBits )
    {
        return bsR( numBits );
    }

    private char readUnsignedChar()
    {
        return (char)bsR( 8 );
    }

    private int readInt()
    {
        int u = 0;
        u = ( u << 8 ) | bsR( 8 );
        u = ( u << 8 ) | bsR( 8 );
        u = ( u << 8 ) | bsR( 8 );
        u = ( u << 8 ) | bsR( 8 );
        return u;
    }

    private int bsR( final int n )
    {
        while( m_bsLive < n )
        {
            char ch = 0;
            try
            {
                ch = (char)m_input.read();
            }
            catch( final IOException ioe )
            {
                compressedStreamEOF();
            }

            if( ch == -1 )
            {
                compressedStreamEOF();
            }

            m_bsBuff = ( m_bsBuff << 8 ) | ( ch & 0xff );
            m_bsLive += 8;
        }

        final int result = ( m_bsBuff >> ( m_bsLive - n ) ) & ( ( 1 << n ) - 1 );
        m_bsLive -= n;
        return result;
    }

    private void bsSetStream( final InputStream input )
    {
        m_input = input;
        m_bsLive = 0;
        m_bsBuff = 0;
    }

    private void complete()
    {
        m_storedCombinedCRC = readInt();
        if( m_storedCombinedCRC != m_computedCombinedCRC )
        {
            crcError();
        }

        bsFinishedWithStream();
        m_streamEnd = true;
    }

    private void endBlock()
    {
        m_computedBlockCRC = m_crc.getFinalCRC();
        /*
         * A bad CRC is considered a fatal error.
         */
        if( m_storedBlockCRC != m_computedBlockCRC )
        {
            crcError();
        }

        m_computedCombinedCRC = ( m_computedCombinedCRC << 1 )
            | ( m_computedCombinedCRC >>> 31 );
        m_computedCombinedCRC ^= m_computedBlockCRC;
    }

    private void hbCreateDecodeTables( final int[] limit,
                                       final int[] base,
                                       final int[] perm,
                                       final char[] length,
                                       final int minLen,
                                       final int maxLen,
                                       final int alphaSize )
    {
        int pp = 0;
        for( int i = minLen; i <= maxLen; i++ )
        {
            for( int j = 0; j < alphaSize; j++ )
            {
                if( length[ j ] == i )
                {
                    perm[ pp ] = j;
                    pp++;
                }
            }
        }

        for( int i = 0; i < MAX_CODE_LEN; i++ )
        {
            base[ i ] = 0;
        }

        for( int i = 0; i < alphaSize; i++ )
        {
            base[ length[ i ] + 1 ]++;
        }

        for( int i = 1; i < MAX_CODE_LEN; i++ )
        {
            base[ i ] += base[ i - 1 ];
        }

        for( int i = 0; i < MAX_CODE_LEN; i++ )
        {
            limit[ i ] = 0;
        }

        int vec = 0;
        for( int i = minLen; i <= maxLen; i++ )
        {
            vec += ( base[ i + 1 ] - base[ i ] );
            limit[ i ] = vec - 1;
            vec <<= 1;
        }

        for( int i = minLen + 1; i <= maxLen; i++ )
        {
            base[ i ] = ( ( limit[ i - 1 ] + 1 ) << 1 ) - base[ i ];
        }
    }

    private void initBlock()
    {
        final char magic1 = readUnsignedChar();
        final char magic2 = readUnsignedChar();
        final char magic3 = readUnsignedChar();
        final char magic4 = readUnsignedChar();
        final char magic5 = readUnsignedChar();
        final char magic6 = readUnsignedChar();
        if( magic1 == 0x17 && magic2 == 0x72 && magic3 == 0x45 &&
            magic4 == 0x38 && magic5 == 0x50 && magic6 == 0x90 )
        {
            complete();
            return;
        }

        if( magic1 != 0x31 || magic2 != 0x41 || magic3 != 0x59 ||
            magic4 != 0x26 || magic5 != 0x53 || magic6 != 0x59 )
        {
            badBlockHeader();
            m_streamEnd = true;
            return;
        }

        m_storedBlockCRC = readInt();

        if( bsR( 1 ) == 1 )
        {
            m_blockRandomised = true;
        }
        else
        {
            m_blockRandomised = false;
        }

        //        currBlockNo++;
        getAndMoveToFrontDecode();

        m_crc.initialiseCRC();
        m_currentState = START_BLOCK_STATE;
    }

    private void initialize()
    {
       final char magic1 = readUnsignedChar();
       final char magic2 = readUnsignedChar();
       final char magic3 = readUnsignedChar();
       final char magic4 = readUnsignedChar();
        if( magic3 != 'h' || magic4 < '1' || magic4 > '9' )
        {
            bsFinishedWithStream();
            m_streamEnd = true;
            return;
        }

        setDecompressStructureSizes( magic4 - '0' );
        m_computedCombinedCRC = 0;
    }

    private void makeMaps()
    {
        m_nInUse = 0;
        for( int i = 0; i < 256; i++ )
        {
            if( m_inUse[ i ] )
            {
                m_seqToUnseq[ m_nInUse ] = (char)i;
                m_unseqToSeq[ i ] = (char)m_nInUse;
                m_nInUse++;
            }
        }
    }

    private void recvDecodingTables()
    {
        buildInUseTable();
        makeMaps();
        final int alphaSize = m_nInUse + 2;

        /*
         * Now the selectors
         */
        final int groupCount = bsR( 3 );
        final int selectorCount = bsR( 15 );
        for( int i = 0; i < selectorCount; i++ )
        {
            int run = 0;
            while( bsR( 1 ) == 1 )
            {
                run++;
            }
            m_selectorMtf[ i ] = (char)run;
        }

        /*
         * Undo the MTF values for the selectors.
         */
        final char[] pos = new char[ N_GROUPS ];
        for( char v = 0; v < groupCount; v++ )
        {
            pos[ v ] = v;
        }

        for( int i = 0; i < selectorCount; i++ )
        {
            int v = m_selectorMtf[ i ];
            final char tmp = pos[ v ];
            while( v > 0 )
            {
                pos[ v ] = pos[ v - 1 ];
                v--;
            }
            pos[ 0 ] = tmp;
            m_selector[ i ] = tmp;
        }

        final char[][] len = new char[ N_GROUPS ][ MAX_ALPHA_SIZE ];
        /*
         * Now the coding tables
         */
        for( int i = 0; i < groupCount; i++ )
        {
            int curr = bsR( 5 );
            for( int j = 0; j < alphaSize; j++ )
            {
                while( bsR( 1 ) == 1 )
                {
                    if( bsR( 1 ) == 0 )
                    {
                        curr++;
                    }
                    else
                    {
                        curr--;
                    }
                }
                len[ i ][ j ] = (char)curr;
            }
        }

        /*
         * Create the Huffman decoding tables
         */
        for( int k = 0; k < groupCount; k++ )
        {
            int minLen = 32;
            int maxLen = 0;
            for( int i = 0; i < alphaSize; i++ )
            {
                if( len[ k ][ i ] > maxLen )
                {
                    maxLen = len[ k ][ i ];
                }
                if( len[ k ][ i ] < minLen )
                {
                    minLen = len[ k ][ i ];
                }
            }
            hbCreateDecodeTables( m_limit[ k ], m_base[ k ], m_perm[ k ], len[ k ], minLen,
                                  maxLen, alphaSize );
            m_minLens[ k ] = minLen;
        }
    }

    private void buildInUseTable()
    {
        final boolean[] inUse16 = new boolean[ 16 ];

        /*
         * Receive the mapping table
         */
        for( int i = 0; i < 16; i++ )
        {
            if( bsR( 1 ) == 1 )
            {
                inUse16[ i ] = true;
            }
            else
            {
                inUse16[ i ] = false;
            }
        }

        for( int i = 0; i < 256; i++ )
        {
            m_inUse[ i ] = false;
        }

        for( int i = 0; i < 16; i++ )
        {
            if( inUse16[ i ] )
            {
                for( int j = 0; j < 16; j++ )
                {
                    if( bsR( 1 ) == 1 )
                    {
                        m_inUse[ i * 16 + j ] = true;
                    }
                }
            }
        }
    }
}

/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */

/**
 * Base class for both the compress and decompress classes. Holds common arrays,
 * and static data.
 *
 * @author <a href="mailto:keiron@aftexsw.com">Keiron Liddle</a>
 */
interface BZip2Constants
{
    int BASE_BLOCK_SIZE = 100000;
    int MAX_ALPHA_SIZE = 258;
    int MAX_CODE_LEN = 23;
    int RUNA = 0;
    int RUNB = 1;
    int N_GROUPS = 6;
    int G_SIZE = 50;
    int N_ITERS = 4;
    int MAX_SELECTORS = ( 2 + ( 900000 / G_SIZE ) );
    int NUM_OVERSHOOT_BYTES = 20;

    int[] RAND_NUMS = new int[]
    {
        619, 720, 127, 481, 931, 816, 813, 233, 566, 247,
        985, 724, 205, 454, 863, 491, 741, 242, 949, 214,
        733, 859, 335, 708, 621, 574, 73, 654, 730, 472,
        419, 436, 278, 496, 867, 210, 399, 680, 480, 51,
        878, 465, 811, 169, 869, 675, 611, 697, 867, 561,
        862, 687, 507, 283, 482, 129, 807, 591, 733, 623,
        150, 238, 59, 379, 684, 877, 625, 169, 643, 105,
        170, 607, 520, 932, 727, 476, 693, 425, 174, 647,
        73, 122, 335, 530, 442, 853, 695, 249, 445, 515,
        909, 545, 703, 919, 874, 474, 882, 500, 594, 612,
        641, 801, 220, 162, 819, 984, 589, 513, 495, 799,
        161, 604, 958, 533, 221, 400, 386, 867, 600, 782,
        382, 596, 414, 171, 516, 375, 682, 485, 911, 276,
        98, 553, 163, 354, 666, 933, 424, 341, 533, 870,
        227, 730, 475, 186, 263, 647, 537, 686, 600, 224,
        469, 68, 770, 919, 190, 373, 294, 822, 808, 206,
        184, 943, 795, 384, 383, 461, 404, 758, 839, 887,
        715, 67, 618, 276, 204, 918, 873, 777, 604, 560,
        951, 160, 578, 722, 79, 804, 96, 409, 713, 940,
        652, 934, 970, 447, 318, 353, 859, 672, 112, 785,
        645, 863, 803, 350, 139, 93, 354, 99, 820, 908,
        609, 772, 154, 274, 580, 184, 79, 626, 630, 742,
        653, 282, 762, 623, 680, 81, 927, 626, 789, 125,
        411, 521, 938, 300, 821, 78, 343, 175, 128, 250,
        170, 774, 972, 275, 999, 639, 495, 78, 352, 126,
        857, 956, 358, 619, 580, 124, 737, 594, 701, 612,
        669, 112, 134, 694, 363, 992, 809, 743, 168, 974,
        944, 375, 748, 52, 600, 747, 642, 182, 862, 81,
        344, 805, 988, 739, 511, 655, 814, 334, 249, 515,
        897, 955, 664, 981, 649, 113, 974, 459, 893, 228,
        433, 837, 553, 268, 926, 240, 102, 654, 459, 51,
        686, 754, 806, 760, 493, 403, 415, 394, 687, 700,
        946, 670, 656, 610, 738, 392, 760, 799, 887, 653,
        978, 321, 576, 617, 626, 502, 894, 679, 243, 440,
        680, 879, 194, 572, 640, 724, 926, 56, 204, 700,
        707, 151, 457, 449, 797, 195, 791, 558, 945, 679,
        297, 59, 87, 824, 713, 663, 412, 693, 342, 606,
        134, 108, 571, 364, 631, 212, 174, 643, 304, 329,
        343, 97, 430, 751, 497, 314, 983, 374, 822, 928,
        140, 206, 73, 263, 980, 736, 876, 478, 430, 305,
        170, 514, 364, 692, 829, 82, 855, 953, 676, 246,
        369, 970, 294, 750, 807, 827, 150, 790, 288, 923,
        804, 378, 215, 828, 592, 281, 565, 555, 710, 82,
        896, 831, 547, 261, 524, 462, 293, 465, 502, 56,
        661, 821, 976, 991, 658, 869, 905, 758, 745, 193,
        768, 550, 608, 933, 378, 286, 215, 979, 792, 961,
        61, 688, 793, 644, 986, 403, 106, 366, 905, 644,
        372, 567, 466, 434, 645, 210, 389, 550, 919, 135,
        780, 773, 635, 389, 707, 100, 626, 958, 165, 504,
        920, 176, 193, 713, 857, 265, 203, 50, 668, 108,
        645, 990, 626, 197, 510, 357, 358, 850, 858, 364,
        936, 638
    };
}


/**
 * A simple class the hold and calculate the CRC for sanity checking of the
 * data.
 *
 * @author <a href="mailto:keiron@aftexsw.com">Keiron Liddle</a>
 */
class CRC
{
    private static int[] CRC32_TABLE = new int[]
    {
        0x00000000, 0x04c11db7, 0x09823b6e, 0x0d4326d9,
        0x130476dc, 0x17c56b6b, 0x1a864db2, 0x1e475005,
        0x2608edb8, 0x22c9f00f, 0x2f8ad6d6, 0x2b4bcb61,
        0x350c9b64, 0x31cd86d3, 0x3c8ea00a, 0x384fbdbd,
        0x4c11db70, 0x48d0c6c7, 0x4593e01e, 0x4152fda9,
        0x5f15adac, 0x5bd4b01b, 0x569796c2, 0x52568b75,
        0x6a1936c8, 0x6ed82b7f, 0x639b0da6, 0x675a1011,
        0x791d4014, 0x7ddc5da3, 0x709f7b7a, 0x745e66cd,
        0x9823b6e0, 0x9ce2ab57, 0x91a18d8e, 0x95609039,
        0x8b27c03c, 0x8fe6dd8b, 0x82a5fb52, 0x8664e6e5,
        0xbe2b5b58, 0xbaea46ef, 0xb7a96036, 0xb3687d81,
        0xad2f2d84, 0xa9ee3033, 0xa4ad16ea, 0xa06c0b5d,
        0xd4326d90, 0xd0f37027, 0xddb056fe, 0xd9714b49,
        0xc7361b4c, 0xc3f706fb, 0xceb42022, 0xca753d95,
        0xf23a8028, 0xf6fb9d9f, 0xfbb8bb46, 0xff79a6f1,
        0xe13ef6f4, 0xe5ffeb43, 0xe8bccd9a, 0xec7dd02d,
        0x34867077, 0x30476dc0, 0x3d044b19, 0x39c556ae,
        0x278206ab, 0x23431b1c, 0x2e003dc5, 0x2ac12072,
        0x128e9dcf, 0x164f8078, 0x1b0ca6a1, 0x1fcdbb16,
        0x018aeb13, 0x054bf6a4, 0x0808d07d, 0x0cc9cdca,
        0x7897ab07, 0x7c56b6b0, 0x71159069, 0x75d48dde,
        0x6b93dddb, 0x6f52c06c, 0x6211e6b5, 0x66d0fb02,
        0x5e9f46bf, 0x5a5e5b08, 0x571d7dd1, 0x53dc6066,
        0x4d9b3063, 0x495a2dd4, 0x44190b0d, 0x40d816ba,
        0xaca5c697, 0xa864db20, 0xa527fdf9, 0xa1e6e04e,
        0xbfa1b04b, 0xbb60adfc, 0xb6238b25, 0xb2e29692,
        0x8aad2b2f, 0x8e6c3698, 0x832f1041, 0x87ee0df6,
        0x99a95df3, 0x9d684044, 0x902b669d, 0x94ea7b2a,
        0xe0b41de7, 0xe4750050, 0xe9362689, 0xedf73b3e,
        0xf3b06b3b, 0xf771768c, 0xfa325055, 0xfef34de2,
        0xc6bcf05f, 0xc27dede8, 0xcf3ecb31, 0xcbffd686,
        0xd5b88683, 0xd1799b34, 0xdc3abded, 0xd8fba05a,
        0x690ce0ee, 0x6dcdfd59, 0x608edb80, 0x644fc637,
        0x7a089632, 0x7ec98b85, 0x738aad5c, 0x774bb0eb,
        0x4f040d56, 0x4bc510e1, 0x46863638, 0x42472b8f,
        0x5c007b8a, 0x58c1663d, 0x558240e4, 0x51435d53,
        0x251d3b9e, 0x21dc2629, 0x2c9f00f0, 0x285e1d47,
        0x36194d42, 0x32d850f5, 0x3f9b762c, 0x3b5a6b9b,
        0x0315d626, 0x07d4cb91, 0x0a97ed48, 0x0e56f0ff,
        0x1011a0fa, 0x14d0bd4d, 0x19939b94, 0x1d528623,
        0xf12f560e, 0xf5ee4bb9, 0xf8ad6d60, 0xfc6c70d7,
        0xe22b20d2, 0xe6ea3d65, 0xeba91bbc, 0xef68060b,
        0xd727bbb6, 0xd3e6a601, 0xdea580d8, 0xda649d6f,
        0xc423cd6a, 0xc0e2d0dd, 0xcda1f604, 0xc960ebb3,
        0xbd3e8d7e, 0xb9ff90c9, 0xb4bcb610, 0xb07daba7,
        0xae3afba2, 0xaafbe615, 0xa7b8c0cc, 0xa379dd7b,
        0x9b3660c6, 0x9ff77d71, 0x92b45ba8, 0x9675461f,
        0x8832161a, 0x8cf30bad, 0x81b02d74, 0x857130c3,
        0x5d8a9099, 0x594b8d2e, 0x5408abf7, 0x50c9b640,
        0x4e8ee645, 0x4a4ffbf2, 0x470cdd2b, 0x43cdc09c,
        0x7b827d21, 0x7f436096, 0x7200464f, 0x76c15bf8,
        0x68860bfd, 0x6c47164a, 0x61043093, 0x65c52d24,
        0x119b4be9, 0x155a565e, 0x18197087, 0x1cd86d30,
        0x029f3d35, 0x065e2082, 0x0b1d065b, 0x0fdc1bec,
        0x3793a651, 0x3352bbe6, 0x3e119d3f, 0x3ad08088,
        0x2497d08d, 0x2056cd3a, 0x2d15ebe3, 0x29d4f654,
        0xc5a92679, 0xc1683bce, 0xcc2b1d17, 0xc8ea00a0,
        0xd6ad50a5, 0xd26c4d12, 0xdf2f6bcb, 0xdbee767c,
        0xe3a1cbc1, 0xe760d676, 0xea23f0af, 0xeee2ed18,
        0xf0a5bd1d, 0xf464a0aa, 0xf9278673, 0xfde69bc4,
        0x89b8fd09, 0x8d79e0be, 0x803ac667, 0x84fbdbd0,
        0x9abc8bd5, 0x9e7d9662, 0x933eb0bb, 0x97ffad0c,
        0xafb010b1, 0xab710d06, 0xa6322bdf, 0xa2f33668,
        0xbcb4666d, 0xb8757bda, 0xb5365d03, 0xb1f740b4
    };

    private int m_globalCrc;

    protected CRC()
    {
        initialiseCRC();
    }

    int getFinalCRC()
    {
        return ~m_globalCrc;
    }

    void initialiseCRC()
    {
        m_globalCrc = 0xffffffff;
    }

    void updateCRC( final int inCh )
    {
        int temp = ( m_globalCrc >> 24 ) ^ inCh;
        if( temp < 0 )
        {
            temp = 256 + temp;
        }
        m_globalCrc = ( m_globalCrc << 8 ) ^ CRC32_TABLE[ temp ];
    }
}