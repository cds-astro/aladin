#++++++++++++++++
#.IDENTIFICATION Makefile
#.LANGUAGE       Make (makefile script)
#.AUTHOR         Francois Ochsenbein [CDS]
#.ENVIRONMENT    Unix
#.KEYWORDS
#.VERSION  1.0   06-Mar-2000
#.PURPOSE        (Re)Create dependent files in this Directory
#.COMMENTS       The script check.sys is assumed to exist.
#               It creates in a local file .sys the `uname -m` value
#               #               followed by the necessary -l options
#----------------

PACK=cds/astro
SHELL=/bin/sh
VERSION=2.0

# Programs:
UP2	= cd `/bin/pwd`/../..
EXEC    = java
#EXEC    = jdb -launch


# Directories: ##########################################
CLASSDIR = ../../../classes


# Files
UTIL    = AstroMath.java Astrotime.java Astroformat.java \
	  Parsing.java Editing.java
COOF    = Coo.java Astrocoo.java Astroframe.java Equatorial.java \
	  ICRS.java FK4.java FK5.java Ecliptic.java \
	  Galactic.java Supergal.java GalGaia.java
COO     = Coo.java Astrocoo.java Astroframe.java Equatorial.java \
	  ICRS.java FK4.java FK5.java Ecliptic.java \
	  Galactic.java Supergal.java Astropos.java
PROJ    = Proj3.java Coocube.java Qbox.java QboxNumber.java 
REGION  = AbstractRegion.java Region.java CircularRegion.java \
	  ConvexRegion.java ZonalRegion.java EllipticalRegion.java
UNIT    = Unit.java Udef.java Converter.java
SRC 	= $(UTIL) $(COO) $(PROJ) $(UNIT) $(REGION) 
#Otype.java

PGM     = tProj3.class tAstroframe.class tQbox.class tOtype.class

# Options:
DEBUG =
#JAVA_VER = `java -version | awk -F'"' '{if(($$2+0)>=5) print "$(JAVA_O5)"}'`
#JAVA_O5  = -Xlint:-unchecked
#JAVA_OPT = -classpath .:../.. -deprecation $(JAVA_VER) -Xlint:unchecked
#JAVA_OPT = -classpath .:../.. -Xlint:-unchecked
#JAVA_OPT = -classpath .:../.. -Xlint:-serial  -Xlint:unchecked
JAVA_OPT = -classpath .:../.. -Xlint:-serial 
JAVAC    = javac $(JAVA_OPT) 


# Dependencies:
all: classes


doc: 
	($(UP2); export LANG=C; javadoc -d ../doc/ cds.astro)
	rm -rf ../../../doc/cds/astro/t*

classes: $(SRC)
	$(JAVAC) -d $(CLASSDIR) $(SRC)
	echo Compiled $? > $@

# TAR

jar: classes
	($(UP2); cd ../classes; jar cf /tmp/cds.astro-$(VERSION).jar cds/astro)

#	$(MAKE) jar ; $(MAKE) doc
export: $(SRC)
	$(MAKE) jar ; 
	-test -d /tmp/java && rm -rf /tmp/java
	mkdir -p /tmp/java/src/cds/astro
	cp -p /tmp/cds.astro-$(VERSION).jar /tmp/java
	cp -p $(SRC) Makefile /tmp/java/src/cds/astro
	tar cf - -C ../../.. doc/cds/astro | (cd /tmp/java ; tar xf -)
	tar cvfz /tmp/cds.astro-$(VERSION).tar.gz -C /tmp/java . 
	ls -l /tmp/cds.astro-$(VERSION).tar.gz
	rm -rf /tmp/java

export-unit: $(UTIL) $(UNIT)
	rm -rf $(CLASSDIR)/*
	-test -d /tmp/cds && rm -rf /tmp/cds
	mkdir -p /tmp/java/src/cds/astro
	$(JAVAC) -d $(CLASSDIR)  $(UTIL) $(UNIT)
	(cd $(CLASSDIR); jar cf /tmp/java/cds.astro$(VERSION)-unit.jar cds/astro)
	cp -p $(UTIL) $(UNIT) Makefile /tmp/java/src/cds/astro
	tar cf - -C ../../.. doc/cds/astro | (cd /tmp/java ; tar xf -)
	tar cvfz /tmp/cds.astro-$(VERSION)-unit.tar.gz -C /tmp/java .
	ls -l /tmp/cds.astro-$(VERSION)-unit.tar.gz
	rm -rf /tmp/java

unit.tar.gz:
	$(UP2) && tar cvfz /tmp/unit.tar.gz \
	  `echo Makefile tUnit.java $(UNIT) $(UTIL) \
	  | sed 's%\([^ ][^ ]*\)%$(PACK)/\1%g'`

# Various TEST Programs

tAstroMath: tAstroMath.java AstroMath.java
	$(JAVAC) $?
	($(UP2); $(EXEC) cds/astro/$@)

tParsing: tParsing.java \
	Astroformat.java Parsing.java AstroMath.java Editing.java
	$(JAVAC) $?
	($(UP2); $(EXEC) cds/astro/$@)

tUnit: tUnit.java \
	Converter.java Unit.java Udef.java \
	Astroformat.java AstroMath.java Parsing.java Editing.java
	$(JAVAC) -deprecation $?
	($(UP2); $(EXEC) cds/astro/$@)

tOtype: tOtype.java Otype.java
	$(JAVAC) -deprecation $?
	($(UP2); $(EXEC) cds/astro/$@)

tConverter: tConverter.java Converter.java \
	Unit.java Udef.java \
	Astroformat.java AstroMath.java Parsing.java Editing.java
	$(JAVAC) -deprecation $?
	($(UP2); $(EXEC) cds/astro/$@)

tCoo2: tCoo2.java Coo2.java AstroMath.java \
	Astroformat.java Parsing.java Editing.java
	$(JAVAC) -Xdiags:verbose $?
	echo "($(UP2); time $(EXEC) x20000000 123-27 130+27</dev/null)"
	($(UP2); $(EXEC) cds/astro/$@)

tCoo: tCoo.java Coo.java AstroMath.java \
	Astroformat.java Parsing.java Editing.java
	$(JAVAC) $?
	echo "($(UP2); time $(EXEC) x20000000 123-27 130+27</dev/null)"
	($(UP2); $(EXEC) cds/astro/$@)

tProj3: tProj3.java Coo.java AstroMath.java Proj3.java \
	Astroformat.java Parsing.java Editing.java
	$(JAVAC) $?
	($(UP2); $(EXEC) cds/astro/$@)

tAstrocoo: tAstrocoo.java $(COOF) $(UTIL)
	$(JAVAC) -g $?
	($(UP2); $(EXEC) cds/astro/$@)

tPierre: tPierre.java $(COO) $(UTIL)
	$(JAVAC) -g $?
	($(UP2); $(EXEC) cds/astro/$@)

tRegion: tRegion.java $(PROJ) $(COO) $(UTIL) $(REGION)
	$(JAVAC) -g $?
	($(UP2); $(EXEC) cds/astro/$@)

tQbox: tQbox.java $(PROJ) $(COO) $(UTIL) $(REGION)
	$(JAVAC) -g $?
	($(UP2); $(EXEC) cds/astro/$@)

tQbox1: tQbox.java $(PROJ) $(COO) $(UTIL) $(REGION) 
	$(JAVAC) -g $?
	($(UP2); $(EXEC) cds/astro/tQbox < cds/astro/tQbox.1 ) | tee /tmp/$@
	gawk -F\( '/===edit_list/{print "\n#", $$0; next}\
	  /^[.][.][.][.]#/{if(NF!=2) next; \
	     pos=$$2; sub(/[()]/, "", pos); print pos}' /tmp/$@ \
	| sed 's/\([+-]\)/ \1/' | aclient vizier 1649 put /tmpx/$@

tQboxNumber: tQboxNumber.java $(PROJ) $(COO) $(UTIL)
	$(JAVAC) -g $?
	($(UP2); $(EXEC) cds/astro/$@)

tQbox2: tQbox2.java $(PROJ) $(COO) $(UTIL)
	$(JAVAC) -g $?
	($(UP2); $(EXEC) cds/astro/$@)

tAstropos: tAstropos.java Astropos.java $(COO) $(UTIL)
	$(JAVAC) -g $?
	($(UP2); $(EXEC) cds/astro/$@)

tPineau: tPineau.java Astropos.java $(COO) $(UTIL)
	$(JAVAC) -g $?
	($(UP2); $(EXEC) cds/astro/$@)

PourFrancois: PourFrancois.java Astropos.java $(COO) $(UTIL)
	$(JAVAC) -g $?
	($(UP2); $(EXEC) cds/astro/$@)

regions.tar.gz: AstroMath.java $(COO) $(PROJ) $(REGION) tQbox.java
	tar cvfz $@ AstroMath.java $(COO) $(PROJ) $(REGION)

clean:
	rm -f *.java~ *.class
