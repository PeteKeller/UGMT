DDIR = ../harntool/

### SOURCE & CODE

$(DDIR)UGMT.jar : $(subst .java,.class,$(wildcard rpg/*.java harn/repository/*.java))
	jar -cfm $@ meta-inf/manifest.mf rpg/*.java rpg/*.class harn/repository/*.java harn/repository/*.class
$(DDIR)calendar/calendar.jar : $(subst .java,.class,$(wildcard harn/calendar/*.java))
	jar -cf $@ harn/calendar/*.class harn/calendar/*.java
$(DDIR)chars/chars.jar : $(subst .java,.class,$(wildcard harn/chars/*.java))
	jar -cf $@ harn/chars/*.class harn/chars/*.java
$(DDIR)combat/combat.jar : $(subst .java,.class,$(wildcard harn/combat/*.java))
	jar -cf $@ harn/combat/*.class harn/combat/*.java
$(DDIR)encounters/encounters.jar : $(subst .java,.class,$(wildcard harn/encounters/*.java))
	jar -cf $@ harn/encounters/*.class harn/encounters/*.java
$(DDIR)equipment/equipment.jar : $(subst .java,.class,$(wildcard harn/equipment/*.java))
	jar -cf $@ harn/equipment/*.class harn/equipment/*.java
$(DDIR)export/export.jar : $(subst .java,.class,$(wildcard harn/export/*.java))
	jar -cf $@ harn/export/*.class harn/export/*.java
$(DDIR)groups/groups.jar : $(subst .java,.class,$(wildcard harn/groups/*.java))
	jar -cf $@ harn/groups/*.class harn/groups/*.java
$(DDIR)index/index.jar : $(subst .java,.class,$(wildcard harn/index/*.java))
	jar -cf $@ harn/index/*.class harn/index/*.java
$(DDIR)log/log.jar : $(subst .java,.class,$(wildcard harn/log/*.java))
	jar -cf $@ harn/log/*.class harn/log/*.java
$(DDIR)magic/magic.jar : $(subst .java,.class,$(wildcard harn/magic/*.java))
	jar -cf $@ harn/magic/*.class harn/magic/*.java
$(DDIR)maps/maps.jar : $(subst .java,.class,$(wildcard harn/maps/*.java))
	jar -cf $@ harn/maps/*.class harn/maps/*.java
$(DDIR)sketch/sketch.jar : $(subst .java,.class,$(wildcard harn/sketch/*.java))
	jar -cf $@ harn/sketch/*.class harn/sketch/*.java
$(DDIR)skills/skills.jar : $(subst .java,.class,$(wildcard harn/skills/*.java))
	jar -cf $@ harn/skills/*.class harn/skills/*.java
$(DDIR)sound/sound.jar : $(subst .java,.class,$(wildcard harn/sound/*.java))
	jar -cf $@ harn/sound/*.class harn/sound/*.java
$(DDIR)travel/travel.jar : $(subst .java,.class,$(wildcard harn/travel/*.java))
	jar -cf $@ harn/travel/*.class harn/travel/*.java
$(DDIR)weather/weather.jar : $(subst .java,.class,$(wildcard harn/weather/*.java))
	jar -cf $@ harn/weather/*.class harn/weather/*.java
$(DDIR)visual/visual.jar : $(subst .java,.class,$(wildcard harn/visual/*.java))
	jar -cf $@ harn/visual/*.class harn/visual/*.java

%.class : %.java ; javac -classpath $(DDIR)jogl.jar:$(DDIR)batik.jar:. $<

### DATA

ABRIEL =    `cd $(DDIR); find . -iwholename "*abriel*" -printf "%P "` # Kaldor
ANGRYL =    `cd $(DDIR); find . -iwholename "*angryl*" -printf "%P "` # Chybisa
ASSORT =    `cd $(DDIR); find . -iwholename "*assort*" -printf "%P "` # Other
ATHELREN =  `cd $(DDIR); find . -iwholename "*athelren*" -printf "%P "` # Kaldor
AVERTU =    `cd $(DDIR); find . -iwholename "*avertu*" -printf "%P "` # Kanday
BURRDAN =   `cd $(DDIR); find . -iwholename "*burrdan*" -printf "%P "` # Kaldor
CALDETH =   `cd $(DDIR); find . -iwholename "*caldeth*" -printf "%P "` # Kaldor
CHARMIC =   `cd $(DDIR); find . -iwholename "*charmic*" -printf "%P "` # Kaldor
CHEWINTIN = `cd $(DDIR); find . -iwholename "*chewintin*" -printf "%P "` # Kaldor
CYRE =      `cd $(DDIR); find . -iwholename "*cyre*" -printf "%P "` # Kaldor
DURZHAN =   `cd $(DDIR); find . -iwholename "*durzhan*" -printf "%P "` # Azadmere
DUREVE =    `cd $(DDIR); find . -iwholename "*dureve*" -printf "%P "` # Kanday
DYRIZA =    `cd $(DDIR); find . -iwholename "*dyriza*" -printf "%P "` # Kanday
MENEKOD =   `cd $(DDIR); find . -iwholename "*menkod*" -printf "%P "` # Kanday
ECB =       `cd $(DDIR); find . -wholename "*ECB*" -printf "%P "` # Kaldor
ELITEN =    `cd $(DDIR); find . -iwholename "*eliten*" -printf "%P "` # Kaldor
EMETE =     `cd $(DDIR); find . -iwholename "*emete*" -printf "%P "` # Kanday
FIGURINE =  `cd $(DDIR); find . -iwholename "*figurine*" -printf "%P "` # Other
GLENOTH2 =  `cd $(DDIR); find . -iwholename "*glenoth2*" -printf "%P "` # Melderyn
GOFFIN =    `cd $(DDIR); find . -iwholename "*goffin*" -printf "%P "` # Kaldor
IRKHAR =    `cd $(DDIR); find . -iwholename "*irkhar*" -printf "%P "` # Azadmere
JEDES =     `cd $(DDIR); find . -iwholename "*jedes*" -printf "%P "` # Kaldor
KEDIS =     `cd $(DDIR); find . -iwholename "*kedis*" -printf "%P "` # Kanday
KOLORN =    `cd $(DDIR); find . -iwholename "*kolorn*" -printf "%P "` # Kaldor
LERIEL =    `cd $(DDIR); find . -iwholename "*leriel*" -printf "%P "` # Orbaal
LORKIN =    `cd $(DDIR); find . -iwholename "*lorkin*" -printf "%P "` # Orbaal
MARBY =     `cd $(DDIR); find . -iwholename "*marby*" -printf "%P "` # Orbaal
MEDARSIN =  `cd $(DDIR); find . -iwholename "*medarsin*" -printf "%P "` # Kaldor
MERCIN =    `cd $(DDIR); find . -iwholename "*mercin*" -printf "%P "` # Azadmere
MONTHEL =   `cd $(DDIR); find . -iwholename "*monthel*" -printf "%P "` # Orbaal
NEBULAN =   `cd $(DDIR); find . -iwholename "*nebulan*" -printf "%P "` # Kaldor
NENDA =     `cd $(DDIR); find . -iwholename "*nenda*" -printf "%P "` # Kaldor
OBEW =      `cd $(DDIR); find . -iwholename "*obew*" -printf "%P "` # Orbaal
RAVIN =     `cd $(DDIR); find . -iwholename "*ravin*" -printf "%P "` # Tharda
SETREW =    `cd $(DDIR); find . -iwholename "*setrew*" -printf "%P "` # Kaldor
SHERWYN =   `cd $(DDIR); find . -iwholename "*sherwyn*" -printf "%P "` # Orbaal
SIRION =    `cd $(DDIR); find . -iwholename "*sirion*" -printf "%P "` # Other
SOPHIA =    `cd $(DDIR); find . -iwholename "*chybisa*" -printf "%P "` # Chybisa
STYBRIN =   `cd $(DDIR); find . -iwholename "*stybrin*" -printf "%P "` # Kaldor
THOEN =     `cd $(DDIR); find . -iwholename "*thoen*" -printf "%P "` # Orbaal
TYANNAL =   `cd $(DDIR); find . -iwholename "*tyannal*" -printf "%P "` # Chybisa
UES =       `cd $(DDIR); find . -wholename "*UES*" -printf "%P "` # Kaldor
WIKI =      `cd $(DDIR); find . -iwholename "*wiki*" -printf "%P "` # Kaldor
WIW =       `cd $(DDIR); find . -iwholename "*WIW*" -printf "%P "` # Kaldor
ZAURIAL =   `cd $(DDIR); find . -iwholename "*zaurial*" -printf "%P "` # Other
ZHAKOM =    `cd $(DDIR); find . -iwholename "*zhakom*" -printf "%P "` # Azadmere
ZUDEN =     `cd $(DDIR); find . -iwholename "*zuden*" -printf "%P "` # Orbaal
K_SPELLS =  `cd $(DDIR); find . -iwholename "*magic/*kevdra*" -printf "%P "` # Spells
C_SPELLS =  `cd $(DDIR); find . -iwholename "*magic/*community*" -printf "%P "` # Spells
L_SPELLS =  `cd $(DDIR); find . -iwholename "*magic/*maillist*" -printf "%P "` # Spells

AZADMERE = $(MERCIN) $(ZHAKOM) $(DURZHAN) $(IRKHAR)
CHYBISA =  $(ANGRYL) $(TYANNAL) $(SOPHIA)
EXAMPLES = `cd $(DDIR); find . -wholename "*/state.xml" -printf "%P "` sketch/synopsis.html log/PCs.html .state
FFF =      `cd $(DDIR); find . -wholename "*FFF*" -printf "%P "`
GENERIC =  `cd $(DDIR); find . -iwholename "*generic*" -printf "%P "`
HARN = 	   `cd $(DDIR); find . -iwholename "*harn*" -printf "%P "`
KALDOR =   $(ABRIEL) $(ATHELREN) $(CALDETH) $(CHARMIC) $(CHEWINTIN) $(CYRE) $(ECB) $(ELITEN) $(KOLORN) $(NEBULAN) $(NENDA) $(SETREW) $(STYBRIN) $(GOFFIN) $(UES) $(WIW) $(MEDARSIN) $(JEDES) $(BURRDAN) $(WIKI)
KELESTIA = `cd $(DDIR); find . -iwholename "*kelestia*" -printf "%P "`
MAPPACK =  `cd $(DDIR); find . -iwholename "*mappack*" -printf "%P "`
ORBAAL =   $(LERIEL) $(LORKIN) $(MARBY) $(MONTHEL) $(SHERWYN) $(THOEN) $(ZUDEN) $(OBEW)
OTHER =    $(GLENOTH2) $(RAVIN) $(KEDIS) $(ASSORT) $(ZAURIAL) $(SIRION) $(AVERTU) $(DYRIZA) $(EMETE) $(MENEKOD) $(DUREVE) $(FIGURINE)
PREVIEW =  maps/Private/Harn.xml maps/Private/Regional index/Private/harn*
PRIVATE =  `cd $(DDIR); find . -iwholename "*private*" -printf "%P "`
JAMES =    */stateJames.xml
SOUNDS =   `cd $(DDIR); find . -wholename "*sound/*sounds*" -printf "%P "`
SPELLS =   $(K_SPELLS) $(C_SPELLS) $(L_SPELLS)
TUOMO =    `cd $(DDIR); find . -iwholename "*tuomo*" -printf "%P "`
LOWSON =   `cd $(DDIR); find . -iwholename "*lowson*" -printf "%P "`
TWG =      `cd $(DDIR); find . -wholename "*TWG*" -printf "%P "`

OEM = *batik* *tritonus* *jorbis* *javazoom* *jogl* lib/*

$(DDIR)Azadmere.zip :
	cd $(DDIR); zip -D -r -q $(notdir $@) $(AZADMERE)
$(DDIR)Chybisa.zip :
	cd $(DDIR); zip -D -r -q $(notdir $@) $(CHYBISA)
$(DDIR)Examples.zip :
	cd $(DDIR); zip -D -r -q $(notdir $@) $(EXAMPLES)
$(DDIR)FFF.zip :
	cd $(DDIR); zip -D -r -q $(notdir $@) $(FFF)
$(DDIR)Generic.zip :
	cd $(DDIR); zip -D -r -q $(notdir $@) $(GENERIC)
$(DDIR)Harn.zip :
	cd $(DDIR); zip -D -r -q $(notdir $@) $(HARN)
$(DDIR)Kaldor.zip :
	cd $(DDIR); zip -D -r -q $(notdir $@) $(KALDOR)
$(DDIR)Kelestia.zip :
	cd $(DDIR); zip -D -r -q $(notdir $@) $(KELESTIA)
$(DDIR)Mappack.zip :
	cd $(DDIR); zip -D -r -q $(notdir $@) $(MAPPACK)
$(DDIR)Orbaal.zip :
	cd $(DDIR); zip -D -r -q $(notdir $@) $(ORBAAL)
$(DDIR)Other.zip :
	cd $(DDIR); zip -D -r -q $(notdir $@) $(OTHER)
$(DDIR)Preview.zip :
	cd $(DDIR); zip -D -r -q $(notdir $@) $(PREVIEW)
$(DDIR)Private.zip :
	cd $(DDIR); zip -D -r -q $(notdir $@) $(PRIVATE)
$(DDIR)James.zip :
	cd $(DDIR); zip -D -r -q $(notdir $@) $(JAMES)
$(DDIR)Sounds.zip :
	cd $(DDIR); zip -D -r -q $(notdir $@) $(SOUNDS)
$(DDIR)Spells.zip :
	cd $(DDIR); zip -D -r -q $(notdir $@) $(SPELLS)
$(DDIR)TWG.zip :
	cd $(DDIR); zip -D -r -q $(notdir $@) $(TWG)
$(DDIR)Tuomo.zip :
	cd $(DDIR); zip -D -r -q $(notdir $@) $(TUOMO)
$(DDIR)Lowson.zip :
	cd $(DDIR); zip -D -r -q $(notdir $@) $(LOWSON)

### PLUGINS & FRAMEWORK

$(DDIR)UGMT.zip : $(DDIR)UGMT.jar $(DDIR)calendar/calendar.jar $(DDIR)chars/chars.jar $(DDIR)combat/combat.jar $(DDIR)encounters/encounters.jar $(DDIR)equipment/equipment.jar $(DDIR)export/export.jar $(DDIR)groups/groups.jar $(DDIR)index/index.jar $(DDIR)log/log.jar $(DDIR)magic/magic.jar $(DDIR)maps/maps.jar $(DDIR)sketch/sketch.jar $(DDIR)skills/skills.jar $(DDIR)sound/sound.jar $(DDIR)travel/travel.jar $(DDIR)weather/weather.jar $(DDIR)visual/visual.jar 
	cd $(DDIR); zip -D -r -q $(notdir $@) README properties $(OEM) UGMT.jar UGMT*.bat UGMT*.sh calendar* chars* combat* encounters* equipment* export* groups* index* log* magic* maps* sketch* skills* sound* travel* weather* visual* -x $(AZADMERE) $(CHYBISA) $(EXAMPLES) $(FFF) $(GENERIC) $(KALDOR) $(KELESTIA) $(MAPPACK) $(ORBAAL) $(OTHER) $(PRIVATE) $(JAMES) $(SOUNDS) $(SPELLS) $(TUOMO) $(TWG) $(LOWSON)

### PHONEYS

all : $(DDIR)UGMT.zip $(DDIR)Azadmere.zip $(DDIR)Chybisa.zip $(DDIR)Examples.zip $(DDIR)FFF.zip $(DDIR)Generic.zip $(DDIR)Harn.zip $(DDIR)Kaldor.zip $(DDIR)Kelestia.zip $(DDIR)Mappack.zip $(DDIR)Orbaal.zip $(DDIR)Other.zip $(DDIR)Preview.zip $(DDIR)Private.zip $(DDIR)James.zip $(DDIR)Sounds.zip $(DDIR)Spells.zip $(DDIR)Tuomo.zip $(DDIR)TWG.zip $(DDIR)Lowson.zip
	cd $(DDIR); zip -D -r -q All.zip * -x *.zip $(KELESTIA) $(PRIVATE) $(EXAMPLES) $(JAMES)

clean :
	find . -name "*.class" -exec rm {} \;
	find . $(DDIR) \( -name "*.zip" -o -name "*~" -o -name "*.back" \) -exec rm {} \;
	rm $(DDIR)*/*.jar
