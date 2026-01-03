# org.eclipse.smarthome.ui charting

The [org.eclipse.smarthome.ui](file:///home/pafoxp/code-openhab/ptrooms_smarthome/bundles/ui/org.eclipse.smarthome.ui) Chart Service is changed
to allow formatting and use formula's on the values of (group)items.

Normally we add defined items to tbe listed as graph.
like http://192.168.1.8:8090/chart?items=<itemname>&theme=black&w=1900&h=900&&service=rrd4j&dpi=135&period=W

Visualisation and size for the graph is done by
* theme=white - black, dark or bright)
* w=480	- width of image
* h=240	- heigth of image
* legend=true (of false) display legend
* dpi=72	- custom DPI or a value between approx 20-400 (more fine to less detail).
Note: dpi=123 will cause a logarithmic graph

The parameter "service="specify the persitence service to use for grabbing values.
* Default of "service=mapdb" uses one single occurance of the last, which reulst in e straigh horizontal line.
* service=rrd4j is more usefull as that has historic Y-axis presence for X-period values .

One of more itemnames (and/or groupnames) seperated by comma, 
can each be prefixed by format values and trailed by a formula: <format><itemname><formula>
By standard openHAB definition: Item & groupnames always start with an alphabetic and followed by one of more aplhanumeric values or underscores.
When using "groups=", the format en formula are valid for all itemnames, part of the group.

We can also use logaritmic display for items greater then zero, by using dpi=123. 
Normally dpiu determines the resulation of the graph low dpi = highres, hig-dpi is less fine grain of pixels.

the "period=" (case sensitive) of the graph (x-as) can be specified as of time now() for a period of:
* h , 8h, 12h	- hours
* D , 2D, 3D	- days
* W, 2W			- weeks
* M, 2M, 4M		- months
* Y, 4Y			- years

We could also specify a dated-time range 
* begin=yyyyMMddHHmm
* end=yyyyMMddHHmm
Note: period can be combined with either "begin=" OR "end=" for that period.
for example: http://192.168.1.8:28090/chart?items=SonoffEsp001Temp&end=202511010000&service=rrd4j&period=M

## Format option
The format values consist of Non-valid items (start)characters: 
*	single numeric 0-9 - used to color the line (0red,1green,2blue,3purple,4orange,5magenta,6red,7black,8yellow,9red)
*	/ (numeric) - painting area the line with color of optional numeric color (0red, 1green, 2blue ,3purple, 4orange,5magenta, 6red, 7grey, 8yellow, 9red)
*	. (one or more decimal points) = place markers on line, each subsequent "." uses the next markers type (round, square, triangle-down, triangle-up)
*	- (one or more dashes) = line type , each subsequent dash uses the next linetype: (none, dash, dash-point, point)
*	< (one or more less) = shifts/starts the X-as sequence 24hours earlier, each next will do 12 hours
*	> (one or more less) = shifts/starts the X-as sequence 24hours later, , each next will do 12 hours

Note: using markers disables manually setting color selection for area.
Note: default/autosequences=Blue, Orange, Purple, Green, Red, Yellow, Pink, Light-Pink, Light-Grey, Magenta, Brown

sample: -->  <>--1/2item3 - graph of item3 is shifted minus (24 plus 12=) 12 hours to left, dash-pointed coloured in green and area below is painted to blue.

## Formula option
A formula is present when the itemname does end on the following operator
*	*	- multiply, followed by decimal number  (item*3 is multiply item value by three)
*	^	- addition, followed by decimal number (tiem^1.1 = item values plus 1.1)
*	|   - divide by item into, followed by decimal number (item|1 = effectivy a reciprocal of 1/item)
*	:	- followed by operator to be added (item1:item2 will produce a graphs of item1 plus values of item2)
*	;	- followed by operator to be subtracted (item1;item2 will produce a graphs of item1 subtracted by item2)
The operator(s) *, ^ and | of the (last) item can either *-multiply or ⁻addition.
Note: order of operation is  divide(into), multiply and then addition

Sample: "item1:item2*3.4^-5.6" will result in multiplication by 3 of sum of item1 and item2 and result is raised by minus 5.6
