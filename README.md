#SnapTwitter

## What is SnapTwitter?
SnapTwitter is an extension for the Snap! programming environment ([snap.berkeley.edu](http://snap.berkeley.edu)).

## What are the requirements for using SnapTwitter?
* Java runtime installed
* internet connection
* Twitter account as this is required for accessing the Twitter API

## How to use SnapTwitter?
1. Start SnapTwitter using ``snaptwitter.bat`` (on Windows) or ``snaptwitter.sh`` (on Linux or OS X)
2. Snap! should load automatically in your default browser with the SnapTwitter blocks preloaded. If this is not the case, start it using http://snap.berkeley.edu/snapsource/snap.html#open:http://localhost:13337/getBlockXML
3. Check if the SnapTwitter blocks were loaded: e.g. there should be a "twitter: prepare" block in the _control_ section

### Example program
The following images show a small example program using the SnapTwitter blocks. First, after pressing the key ``p``, it establishes a connection to Twitter and initiates the authentication mechanisms (``twitter: prepare``). After pressing ``space``, it connects to the twitter stream (``twitter: connect to stream``), initializes a map view (``twitter: stamp map image``) and then processes all received tweets usijng the ``twitter: for each tweet``. All these tweets are visualized on the map using the ``twitter: show tweet ___ on map`` block, until the key ``s`` is being pressed and the program disconnects from the twitter stream (``twitter: disconnect from stream``)

![Image of example blocks 1](http://fau-ddi.github.io/SnapTwitter/images/example-block1.png) ![Image of example blocks 2](http://fau-ddi.github.io/SnapTwitter/images/example-block2.png) ![Image of example blocks 3](http://fau-ddi.github.io/SnapTwitter/images/example-block3.png) 


## Contact
Andreas Grillenberger  
Computing Education Research Group  
Friedrich-Alexander-Universität Erlangen-Nürnberg  
andreas.grillenberger@fau.de
