Marques Everett Mondliwethu Mthunzi - 119119
Lars Meyer - 114719

A compiled version of the app can be found in app/release/app-release.apk

We've attempted to enhance the text input field with location autocomplete, but did not manage to finish this. Therefore, the "i"-Button is not functional, but will be functional at a later stage.

On the bottom right, there is a "Clear all markers" button - it not only clears the displayed, but also all the saved markers.

To create a polygon, press the "Start Polygon" button. This enables "polygon capture" mode, where every marker created gets added to the polygon, which will be drawn upon tapping "End Polygon". A marker will then appear in the centroid of all vertices, showing the calculated area.

References for the polygon area and centroid calculation (as well as for other code snippets) are found in the code comments above the respective methods.
