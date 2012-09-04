package com.geodesy.seminar.genttiindoormap;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.ericsson.android.indoormaps.IndoorMapActivity;
import com.ericsson.android.indoormaps.ItemizedOverlay;
import com.ericsson.android.indoormaps.MapController;
import com.ericsson.android.indoormaps.MapController.LoadingListener;
import com.ericsson.android.indoormaps.MapController.MapItemOnFocusChangeListener;
import com.ericsson.android.indoormaps.MapManager;
import com.ericsson.android.indoormaps.MapView;
import com.ericsson.android.indoormaps.MyLocationOverlay;
import com.ericsson.android.indoormaps.Overlay;
import com.ericsson.android.indoormaps.OverlayItem;
import com.ericsson.android.indoormaps.Projection;
import com.ericsson.android.indoormaps.location.IndoorLocationProvider;
import com.ericsson.android.indoormaps.location.IndoorLocationProvider.IndoorLocationListener;
import com.ericsson.android.indoormaps.location.IndoorLocationProvider.IndoorLocationRequestStatus;
import com.ericsson.indoormaps.model.BuildingDescription;
import com.ericsson.indoormaps.model.GeoPoint;
import com.ericsson.indoormaps.model.Location;
import com.ericsson.indoormaps.model.MapDescription;
import com.ericsson.indoormaps.model.MapItem;
import com.ericsson.indoormaps.model.Point;

public class IndoorMapGentti extends IndoorMapActivity implements
		LoadingListener, View.OnClickListener {

	private static final int GENTTI_BUILDING_ID = 5738;
	private static final int GENTTI_MAP_ID = 1507;
	private static final int GENTTI_STYLE_ID = 399;

	private static final String API_KEY = "QdQPEKKpg1vRnmgDaELDBc5mnKGuewiB9B3YZemd";
	private static final String LOG_TAG = "GenttiIndoorMapClient";

	private MapController mMapController;
	// private Route mRoute;

	private MyLocationOverlay mMyLocationOverlay;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		setMapView((MapView) findViewById(R.id.indoor_map_view));
		mMapController = getMapView().getMapController();
		mMapController.setLoadingListener(this);
		mMapController.setMap(GENTTI_MAP_ID, API_KEY, true);
		mMapController.setStyle(GENTTI_STYLE_ID, API_KEY, true);

		// Zoom controls
		getMapView().setBuiltInZoomControls(true);

		// Listen to focus change on map
		mMapController
				.setOnFocusChangeListener(new MapItemOnFocusChangeListener() {

					@Override
					public void onMapItemFocusChange(MapItem mapItem) {
						if (mapItem != null) {
							Map<String, String> tags = mapItem.getTags();
							String text = "Tags : " + tags;
							toast(text);
						} else {
							toast("Unknown map item or out of focus!");
						}
					}
				});
	} // onCreate

	/**
	 * An ItemizedOverlay class that is customized to listen to a tap and toast
	 * its tags.
	 **/
	private class CustomItemizedOverlay extends ItemizedOverlay {
		@Override
		public boolean singleTap(OverlayItem item) {
			if (super.singleTap(item)) {
				toast(item.getText());
				return true;
			}
			return false;
		}
	}

	public void testClick(View v) {
		// Select those rooms with tags "room":"yes"
		final HashMap<String, String> roomTags = new HashMap<String, String>();
		roomTags.put("room", "yes");
		final List<MapItem> mapItems = MapManager.getMapItems(roomTags,
				GENTTI_MAP_ID, getApplicationContext());
		mMapController.setSelected(mapItems);

		// Get Overlay
		List<Overlay> mapOverlays = mMapController.getOverlays();

		// Creating itemized overlay
		CustomItemizedOverlay overlay = new CustomItemizedOverlay();
		Drawable defaultIcon = getResources().getDrawable(
				R.drawable.ic_launcher);
		overlay.setDefaultMarker(defaultIcon);

		// Adding OverlayItem to use the defaultIcon
		Point point = new Point(100, 10);
		OverlayItem item = new OverlayItem(point, null, "Overlay Item 1");
		overlay.addItem(item);
		mapOverlays.add(overlay);
		getDescriptionsAsync();
	}

	private void getDescriptionsAsync() {
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				// Single map description
				MapDescription mapDescription;
				try {
					mapDescription = MapManager.getMapDescription(
							GENTTI_MAP_ID, API_KEY, IndoorMapGentti.this);
					Log.d(LOG_TAG, mapDescription.toString());
				} catch (IOException e) {
					Log.e(LOG_TAG,
							"Map Description returned error: " + e.getMessage(),
							e);
				}

				// Single building description
				BuildingDescription buildingDescription;
				try {
					buildingDescription = MapManager.getBuildingDescription(
							GENTTI_BUILDING_ID, API_KEY, IndoorMapGentti.this);
					Log.d(LOG_TAG, buildingDescription.toString());
				} catch (IOException e) {
					Log.e(LOG_TAG,
							"Building description returned error: "
									+ e.getMessage(), e);
				}

				try {
					Context c = getApplicationContext();
					// Map descriptions for all maps cached on device
					for (MapDescription b : MapManager.getMapDescriptions(c,
							true, API_KEY)) {
						Log.d(LOG_TAG, "Maps - On device: " + b.toString());
					}

					// Get map descriptions for all maps accessible for your API
					// key on the server
					for (MapDescription b : MapManager.getMapDescriptions(c,
							false, API_KEY)) {
						Log.d(LOG_TAG, "Maps - On server: " + b.toString());
					}

				} catch (IOException e) {
					Log.d(LOG_TAG, "Couldn't retrieve info from server", e);
				}
				return null;
			}
		}.execute();
	}

	class CustomOverlay extends Overlay {
		private final GestureDetector mGestureDetector;
		private MapView mTouchedMapView;

		@SuppressWarnings("deprecation")
		public CustomOverlay() {
			mGestureDetector = new GestureDetector(new MyGestureListener());
		}

		@Override
		public void draw(Canvas canvas, MapView mapView) {
			MapItem focusedMapItem = mMapController.getFocusedMapItem();
			if (focusedMapItem != null) {
				Projection projection = mapView.getProjection();
				float canvasX = projection.getCanvasCoord(focusedMapItem
						.getCenter().getX());
				float canvasY = projection.getCanvasCoord(focusedMapItem
						.getCenter().getY());

				Paint p = new Paint();
				p.setColor(Color.BLUE);
				p.setAlpha(90);
				p.setAntiAlias(true);
				canvas.drawCircle(canvasX, canvasY, 10, p);
			}
		}

		class MyGestureListener extends SimpleOnGestureListener {
			@Override
			public void onLongPress(MotionEvent e){
				Projection p = mTouchedMapView.getProjection();
				Point point = new Point(p.getMapX(e.getX()), p.getMapY(e.getY()));
				Location fakeLocation = new Location(point, getMapView().getBuildingId(), getMapView().getFloorId());
				addMyLocationOverlayIfMissing(); 
				mMyLocationOverlay.setLocation(fakeLocation);
				mMyLocationOverlay.setShowAccuracy(false);
				mTouchedMapView.invalidate();
				Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				v.vibrate(100);
				super.onLongPress(e);
			}
		}

	}

	private void toast(String text) {
		Toast.makeText(this, text, Toast.LENGTH_LONG).show();
	}

	public void requestLocation(View v) {
		// Set up indoor location api and request location
		IndoorLocationProvider locationProvider = new IndoorLocationProvider();
		IndoorLocationListener locationListener = new IndoorLocationListener() {

			@Override
			public void onError(IndoorLocationRequestStatus status,
					String message) {
				Log.d(LOG_TAG, "Error: " + message + " status" + status);
				toast("Location not found: " + message);
			}

			@Override
			public void onIndoorLocation(double lat, double lon,
					int buildingID, int floorID, int horizontalAccuracy) {
				GeoPoint geoPoint = new GeoPoint(lat, lon);
				geoPoint.setBuildingId(buildingID);
				geoPoint.setFloorId(floorID);
				Location location = getMapView().getProjection().getLocation(
						geoPoint);

				if (location != null) {
					addMyLocationOverlayIfMissing();
					mMyLocationOverlay.setLocation(location);
					mMyLocationOverlay.setAccuracy(horizontalAccuracy);
					mMyLocationOverlay.setShowAccuracy(true);
					mMapController.animateTo(mMyLocationOverlay.getLocation()
							.getPoint());
				}
				Log.d(LOG_TAG, "Indoor location " + geoPoint);
				toast("Location recieved: " + geoPoint);

			}
		};

		locationProvider.requestIndoorLocation(locationListener, this, API_KEY);
	}

	private void addMyLocationOverlayIfMissing() {
		if (mMyLocationOverlay == null) {
			mMyLocationOverlay = new MyLocationOverlay();
			mMapController.getOverlays().add(mMyLocationOverlay);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.buttonTest:
			testClick(v);
			break;
		case R.id.buttonLocation:
			requestLocation(v);
			break;
		default:
			break;
		}
	}

	@Override
	public void onMapLoading(LoadingState state, int mapID, String message) {
		Log.d(LOG_TAG, "IndoorMapGentti.onMapLoading() STATE "+ state +" MESSAGE "+ message);
		
		switch(state){
		case FINISHED:
			List<Overlay> mapOverlays = mMapController.getOverlays(); 
			showCaseProjection();
			CustomOverlay customOverlay = null;
			for(Overlay o: mapOverlays){
				if(o instanceof CustomOverlay){
					customOverlay = (CustomOverlay) o;
					break;
				}
			}
			break;
		case ERROR:
			Toast.makeText(IndoorMapGentti.this, message, Toast.LENGTH_LONG).show();
			break;
		default:
			break;
		}
	}
	
	private void showCaseProjection(){
		ItemizedOverlay overlay = new ItemizedOverlay();
		double latitude = 60.188985;
		double longitude = 24.808610;
		Location locationFromLonLat = getMapView().getProjection().getLocation(new GeoPoint(latitude, longitude));
		if(locationFromLonLat != null){
			Point point = locationFromLonLat.getPoint();
			overlay.addItem(new OverlayItem(point, getResources().getDrawable(R.drawable.ic_launcher), null));
			getMapView().getMapController().getOverlays().add(overlay);
		}
	}
	

	@Override
	public void onStyleLoading(LoadingState state, int styleID, String message) {
		Log.d(LOG_TAG, "IndoorMapGentti.onStyleLoading() - STATE " + state );
		if(state == LoadingState.ERROR){
			Toast.makeText(this, message, Toast.LENGTH_LONG).show();
		}
	}
}



















