#!/bin/sh

LINES=$(cat lines.txt | sed -e 's/ /_/g' | cut -d')' -f1 | cut -d'(' -f2 | grep ^id= | cut -d'=' -f2 | sort -n)

echo '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<gpx xmlns="http://www.topografix.com/GPX/1/1" version="1.1" creator="FilBleuExporter">
<metadata>Filbleu</metadata>';

for LINE in $LINES; do
	name=$(grep "(id=$LINE)" lines.txt | sed -e "s/Line(id=$LINE): //g" -e 's/name={.*} //g' -e 's/name={.*}$//g' -e 's/number=//g' -e 's/;//g' -e 's/ | /, /g' -e 's/ $//g')
	echo "	<trk>"
	echo "		<name>Ligne $name</name>"
	echo "		<trkseg>"
	for stop in $(cat stops.$LINE.txt | sed -e 's/ /_/g' | cut -d'=' -f2 | sed -e 's/^>_//g'); do
		# line:
		# Found_a_stop_matching_stopArea:_[StopArea|676|Voltaire|Tours|||475666,50|2267709,00|1598!0!14;1597!0!14;];_Lambert2+:_{E:475666.500000,_N:2267709.000000};_Degrees:_{E:47.397322,_N:0.689295}
		line=$(grep "$(echo $stop | sed -e 's/_/ /g')" stops_coords.txt | sed -e 's/ /_/g');
		arret=$(echo $line | cut -d'|' -f3 | sed -e 's/_/ /g');
		latlon=$(echo $line | sed -e 's/.*_Degrees:_//g' -e 's/{//g' -e 's/}//g')
		lat=$(echo $latlon | cut -d',' -f1 | cut -d':' -f2 | sed -e 's/_/ /g');
		lon=$(echo $latlon | cut -d',' -f2 | cut -d':' -f2 | sed -e 's/_/ /g');
		echo "		<trkpt lon=\"$lon\" lat=\"$lat\">"
		echo "			<ele>0.0</ele>"
		echo "			<name>$arret</name>"
		echo "		</trkpt>"
	done;
	echo "		</trkseg>"
done;

echo '</gpx>'
