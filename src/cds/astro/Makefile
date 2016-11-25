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
VERSION=1.50

# Programs:
UP2	= cd `/bin/pwd`/../..
EXEC    = java
#EXEC    = jdb -launch


# Directories: ##########################################
CLASSDIR = ../../classes


# Files
UTIL    = AstroMath.java Astrotime.java Astroformat.java \
	  Parsing.java Editing.java
COO     = Coo.java Astrocoo.java Astroframe.java Equatorial.java \
	  ICRS.java FK4.java FK5.java Ecliptic.java \
	  Galactic.java Supergal.java Astropos.java
PROJ    = Proj3.java Coocube.java Qbox.java QboxNumber.java 
REGION  = AbstractRegion.java Region.java CircularRegion.java \
	  ConvexRegion.java ZonalRegion.java EllipticalRegion.java
UNIT    = Unit.java Udef.java Converter.java
SRC 	= $(UTIL) $(COO) $(PROJ) $(UNIT) $(REGION) 
#Otype.java

PGM     = t-Proj3.class t-Astroframe.class t-Qbox.class 

# Options:
DEBUG =
#JAVA_VER = `java -version | awk -F'"' '{if(($$2+0)>=5) print "$(JAVA_O5)"}'`
#JAVA_O5  = -Xlint:-unchecked
#JAVA_OPT = -classpath .:../.. -deprecation $(JAVA_VER) -Xlint:unchecked
JAVA_OPT = -classpath .:../.. -Xlint:-unchecked
JAVAC    = javac $(JAVA_OPT) 


# Dependencies:
all: classes jar


doc: 
	($(UP2); export LANG=C; javadoc -encoding UTF-8 -d doc/ cds.astro)
	rm -rf ../../doc/cds/astro/t*

classes: $(SRC)
	test -d $(CLASSDIR) || mkdir $(CLASSDIR)
	$(JAVAC) -d $(CLASSDIR) $(SRC)
	echo Compiled $? > $@

# TAR

jar: classes
	( cd $(CLASSDIR); jar cf /tmp/cds.astro-$(VERSION).jar cds/astro)
	cp -p /tmp/cds.astro-$(VERSION).jar ../.. 
	cd ../.. && rm -f cds.astro.jar \
	&& ln -s cds.astro-$(VERSION).jar cds.astro.jar
	#($(UP2); cd classes; jar cf /tmp/cds.astro-$(VERSION).jar cds/astro)

export: $(SRC)
	$(MAKE) jar ; $(MAKE) doc
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
	  `echo Makefile t-Unit.java $(UNIT) $(UTIL) \
	  | sed 's%\([^ ][^ ]*\)%$(PACK)/\1%g'`

# Various TEST Programs

t-AstroMath: t-AstroMath.java AstroMath.java
	$(JAVAC) $?
	($(UP2); $(EXEC) cds/astro/$@)

t-Parsing: t-Parsing.java \
	Astroformat.java Parsing.java AstroMath.java Editing.java
	$(JAVAC) $?
	($(UP2); $(EXEC) cds/astro/$@)

t-Unit: t-Unit.java \
	Converter.java Unit.java Udef.java \
	Astroformat.java AstroMath.java Parsing.java Editing.java
	$(JAVAC) -deprecation $?
	($(UP2); $(EXEC) cds/astro/$@)

#t-Otype: t-Otype.java Otype.java
#	$(JAVAC) -deprecation $?
#	($(UP2); $(EXEC) cds/astro/$@)

t-Converter: t-Converter.java Converter.java \
	Unit.java Udef.java \
	Astroformat.java AstroMath.java Parsing.java Editing.java
	$(JAVAC) -deprecation $?
	($(UP2); $(EXEC) cds/astro/$@)

t-Coo: t-Coo.java Coo.java AstroMath.java \
	Astroformat.java Parsing.java Editing.java
	$(JAVAC) $?
	($(UP2); $(EXEC) cds/astro/$@)

t-Proj3: t-Proj3.java Coo.java AstroMath.java Proj3.java \
	Astroformat.java Parsing.java Editing.java
	$(JAVAC) $?
	($(UP2); $(EXEC) cds/astro/$@)

tAstrocoo: t-Astrocoo.java $(COO) $(UTIL)
	$(JAVAC) -g $@.java $(COO) $(UTIL)
	($(UP2); $(EXEC) cds/astro/$@)

t-Astrocoo: t-Astrocoo.java $(COO) $(UTIL)
	$(JAVAC) -g $?
	($(UP2); $(EXEC) cds/astro/$@)

t-Pierre: t-Pierre.java $(COO) $(UTIL)
	$(JAVAC) -g $?
	($(UP2); $(EXEC) cds/astro/$@)

t-Region: t-Region.java $(PROJ) $(COO) $(UTIL) $(REGION)
	$(JAVAC) -g $?
	($(UP2); $(EXEC) cds/astro/$@)

t-Qbox: t-Qbox.java $(PROJ) $(COO) $(UTIL) $(REGION)
	$(JAVAC) -g $?
	($(UP2); $(EXEC) cds/astro/$@)

t-Qbox1: t-Qbox.java $(PROJ) $(COO) $(UTIL) $(REGION) 
	$(JAVAC) -g $?
	($(UP2); $(EXEC) cds/astro/t-Qbox < cds/astro/t-Qbox.1 ) | tee /tmp/$@
	gawk -F\( '/===edit_list/{print "\n#", $$0; next}\
	  /^[.][.][.][.]#/{if(NF!=2) next; \
	     pos=$$2; sub(/[()]/, "", pos); print pos}' /tmp/$@ \
	| sed 's/\([+-]\)/ \1/' | aclient vizier 1649 put /tmpx/$@

t-QboxNumber: t-QboxNumber.java $(PROJ) $(COO) $(UTIL)
	$(JAVAC) -g $?
	($(UP2); $(EXEC) cds/astro/$@)

t-Qbox2: t-Qbox2.java $(PROJ) $(COO) $(UTIL)
	$(JAVAC) -g $?
	($(UP2); $(EXEC) cds/astro/$@)

t-Astropos: t-Astropos.java Astropos.java $(COO) $(UTIL)
	$(JAVAC) -g $?
	($(UP2); $(EXEC) cds/astro/$@)

t-Pineau: t-Pineau.java Astropos.java $(COO) $(UTIL)
	$(JAVAC) -g $?
	($(UP2); $(EXEC) cds/astro/$@)

PourFrancois: PourFrancois.java Astropos.java $(COO) $(UTIL)
	$(JAVAC) -g $?
	($(UP2); $(EXEC) cds/astro/$@)

regions.tar.gz: AstroMath.java $(COO) $(PROJ) $(REGION) t-Qbox.java
	tar cvfz $@ AstroMath.java $(COO) $(PROJ) $(REGION)

clean:
	rm -f *.java~ *.class classes
