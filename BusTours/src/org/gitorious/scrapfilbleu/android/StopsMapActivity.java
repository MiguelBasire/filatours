/* vim: set ts=4 sw=4 et: */

package org.gitorious.scrapfilbleu.android;

import java.util.ArrayList;

import android.util.Log;

import android.os.Bundle;

import android.location.Location;

import android.widget.Toast;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;

public class StopsMapActivity extends MapViewActivity
{
	private String[] stopsNames;
    private double[] latitudes;
    private double[] longitudes;
    private Location whereAmI;
    private BoundingBoxE6 bbox;

    private ItemizedOverlay<OverlayItem> mMyLocationOverlay;
    private ResourceProxy mResourceProxy;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        double bboxNorth = 0, bboxEast = 0, bboxSouth = 0, bboxWest = 0;
        super.onCreate(savedInstanceState);

        this.mResourceProxy = new DefaultResourceProxyImpl(getApplicationContext());
        ArrayList<OverlayItem> items = new ArrayList<OverlayItem>();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            this.stopsNames = extras.getStringArray("stopsNames");
            this.latitudes = extras.getDoubleArray("latitudes");
            this.longitudes = extras.getDoubleArray("longitudes");
            this.whereAmI = (Location)extras.get("location");
        }

        if (this.stopsNames != null && this.latitudes != null && this.longitudes != null) {
            if (this.stopsNames.length == this.latitudes.length && this.latitudes.length == this.longitudes.length && this.longitudes.length > 0) {
                Log.e("BusTours:StopsMaps", "Got some points ...");
                int i;
                double minNorth = 180, minEast = 180, maxSouth = -180, maxWest = -180;
                for (i = 0; i < this.stopsNames.length; i++) {
                    Log.e("BusTours:StopsMaps", "Stops @ (" + String.valueOf(this.latitudes[i]) + ", " + String.valueOf(this.longitudes[i]) + ") == " + this.stopsNames[i]);
                    items.add(new OverlayItem(this.stopsNames[i], this.stopsNames[i], new GeoPoint((int)(this.latitudes[i]*1e6), (int)(this.longitudes[i]*1e6))));
                    if (this.latitudes[i] < minEast) {
                        minEast = this.latitudes[i];
                    }
                    if (this.latitudes[i] > maxWest) {
                        maxWest = this.latitudes[i];
                    }
                    if (this.longitudes[i] < minNorth) {
                        minNorth = this.longitudes[i];
                    }
                    if (this.longitudes[i] > maxSouth) {
                        maxSouth = this.longitudes[i];
                    }
                }
                Log.e("BusTours:StopsMaps", "minEast=" + String.valueOf(minEast));
                Log.e("BusTours:StopsMaps", "maxWest=" + String.valueOf(maxWest));
                Log.e("BusTours:StopsMaps", "minNorth=" + String.valueOf(minNorth));
                Log.e("BusTours:StopsMaps", "maxSouth=" + String.valueOf(maxSouth));
                bboxNorth = minNorth;
                bboxEast = minEast;
                bboxSouth = maxSouth;
                bboxWest = maxWest;
            }
        }

        /* OnTapListener for the Markers, shows a simple Toast. */
        this.mMyLocationOverlay = new ItemizedIconOverlay<OverlayItem>(items,
            new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                @Override
                public boolean onItemSingleTapUp(final int index,
                        final OverlayItem item) {
                    Toast.makeText(
                            StopsMapActivity.this,
                            item.mTitle, Toast.LENGTH_LONG).show();
                    return true; // We 'handled' this event.
                }
                @Override
                public boolean onItemLongPress(final int index,
                        final OverlayItem item) {
                    Toast.makeText(
                            StopsMapActivity.this,
                            item.mTitle ,Toast.LENGTH_LONG).show();
                    return false;
                }
            }, mResourceProxy);

        this.getOsmMap().getOverlays().add(this.mMyLocationOverlay);
        /* Inverting coords, otherwise buggy results ?! */
        this.bbox = new BoundingBoxE6(bboxEast, bboxNorth, bboxWest, bboxSouth);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus)
    {
        if (hasFocus) {
            Log.e("BusTours:StopsMap", "Got focus, zooming to " + this.bbox);
            Log.e("BusTours:StopsMap", "Targeting &lat=" + (this.bbox.getCenter().getLatitudeE6()/1E6) + "&lon=" + (this.bbox.getCenter().getLongitudeE6()/1E6) + "&zoom=15");
            this.getOsmMap().getController().zoomToSpan(this.bbox);
            this.getOsmMap().getController().setCenter(this.bbox.getCenter());
        }
    }
}
