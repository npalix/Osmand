package net.osmand.plus.views.controls;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.osmand.CallbackWithObject;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SettingsBaseActivity;
import net.osmand.plus.activities.SettingsNavigationActivity;
import net.osmand.plus.activities.actions.AppModeDialog;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.router.GeneralRouter;
import net.osmand.router.GeneralRouter.RoutingParameter;
import net.osmand.router.GeneralRouter.RoutingParameterType;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Canvas;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

public class MapRoutePreferencesControl extends MapControls {
	private ImageButton settingsAppModeButton;
	private OsmandSettings settings;
	private int cachedId;
	private Dialog dialog;
	private GPXFile selectedGPXFile = null;
	
	public MapRoutePreferencesControl(MapActivity mapActivity, Handler showUIHandler, float scaleCoefficient) {
		super(mapActivity, showUIHandler, scaleCoefficient);
		settings = mapActivity.getMyApplication().getSettings();
	}
	
	@Override
	public void showControls(FrameLayout parent) {
		settingsAppModeButton = addImageButton(parent, R.string.route_preferences, R.drawable.map_btn_plain);
		cachedId = 0;
		settingsAppModeButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(dialog != null) {
					dialog.hide();
					dialog = null;
					settingsAppModeButton.setBackgroundResource(R.drawable.map_btn_plain);
				} else {
					dialog = showDialog();
					dialog.show();
					settingsAppModeButton.setBackgroundResource(R.drawable.map_btn_plain_p);
					dialog.setOnDismissListener(new OnDismissListener() {
						@Override
						public void onDismiss(DialogInterface dlg) {
							settingsAppModeButton.setBackgroundResource(R.drawable.map_btn_plain);
							dialog = null;
						}
					});
				}
			}
		});
	}
	
	private Dialog showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mapActivity);        
        View ll = createLayout();
        builder.setView(ll);
        //builder.setTitle(R.string.route_preferences);
        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.BOTTOM;
        lp.y = (int) (settingsAppModeButton.getBottom() - settingsAppModeButton.getTop() + scaleCoefficient * 5); 
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setAttributes(lp);
        return dialog;
	}

	
	private List<RoutingParameter> getRoutingParameters(ApplicationMode am) {
		List<RoutingParameter> list = new ArrayList<RoutingParameter>();
		GeneralRouter rm = SettingsNavigationActivity.getRouter(am);
		if(rm == null) {
			return list;
		}
		for (RoutingParameter r : rm.getParameters().values()) {
			if (r.getType() == RoutingParameterType.BOOLEAN) {
				list.add(r);
			}
		}
		return list;
	}
	private View createLayout() {
		View settingsDlg = View.inflate(mapActivity, R.layout.plan_route_settings, null);
		Context ctx = mapActivity;
		final OsmandSettings settings = mapActivity.getMyApplication().getSettings();
		ApplicationMode am = settings.APPLICATION_MODE.get();
		final ListView lv = (ListView) settingsDlg.findViewById(android.R.id.list);
		final Set<ApplicationMode> selected = new HashSet<ApplicationMode>();
		selected.add(am);
		
		setupSpinner(settingsDlg);
		
		
		final ArrayAdapter<RoutingParameter> listAdapter = new ArrayAdapter<RoutingParameter>(ctx, 
				R.layout.layers_list_activity_item, R.id.title, getRoutingParameters(am)) {
			@Override
			public View getView(final int position, View convertView, ViewGroup parent) {
				View v = mapActivity.getLayoutInflater().inflate(R.layout.layers_list_activity_item, null);
				final TextView tv = (TextView) v.findViewById(R.id.title);
				final CheckBox ch = ((CheckBox) v.findViewById(R.id.check_item));
				RoutingParameter rp = getItem(position);
				tv.setText(SettingsBaseActivity.getRoutingStringPropertyName(mapActivity, rp.getId(), rp.getName()));
				tv.setPadding((int) (5 * scaleCoefficient), 0, 0, 0);
				final CommonPreference<Boolean> property = settings.getCustomRoutingBooleanProperty(rp.getId());
				ch.setChecked(property.get());
				ch.setVisibility(View.VISIBLE);
				ch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						property.set(isChecked);
						mapActivity.getRoutingHelper().recalculateRouteDueToSettingsChange();
					}
				});
				return v;
			}
		};
		
		AppModeDialog.prepareAppModeView(mapActivity, selected, false, 
				(ViewGroup) settingsDlg.findViewById(R.id.TopBar), true, 
				new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(selected.size() > 0) {
					ApplicationMode next = selected.iterator().next();
					settings.APPLICATION_MODE.set(next);
					mapActivity.getRoutingHelper().recalculateRouteDueToSettingsChange();
					listAdapter.setNotifyOnChange(false);
					listAdapter.clear();
					for(RoutingParameter r : getRoutingParameters(next)) {
						listAdapter.add(r);
					}
					listAdapter.notifyDataSetChanged();
				}
			}
		});
		lv.setAdapter(listAdapter);
		return settingsDlg;
	}

	private void setupSpinner(View settingsDlg) {
		final Spinner gpxSpinner = (Spinner) settingsDlg.findViewById(R.id.GPXRouteSpinner);
		updateSpinnerItems(gpxSpinner);
		gpxSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if(position == 0) {
					selectedGPXFile = null;
				} else if(position == 1) {
					openGPXFileSelection(gpxSpinner);
				} else if(position == 2) {
					// nothing to change 
				}				
				
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
	}

	protected void openGPXFileSelection(final Spinner gpxSpinner) {
		GpxUiHelper.selectGPXFile(mapActivity, false, false, new CallbackWithObject<GPXUtilities.GPXFile>() {
			
			@Override
			public boolean processResult(GPXFile result) {
				selectedGPXFile = result;
				updateSpinnerItems(gpxSpinner);
				return true;
			}
		});
	}

	private void updateSpinnerItems(Spinner gpxSpinner) {
		ArrayList<String> gpxActions = new ArrayList<String>();
		gpxActions.add(mapActivity.getString(R.string.default_none));
		gpxActions.add(mapActivity.getString(R.string.select_gpx));
		if(selectedGPXFile != null) {
			gpxActions.add(new File(selectedGPXFile.path).getName());
		}
		
		ArrayAdapter<String> gpxAdapter = new ArrayAdapter<String>(mapActivity, 
				android.R.layout.simple_spinner_item, 
				gpxActions
				);
		gpxAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		gpxSpinner.setAdapter(gpxAdapter);
		if(selectedGPXFile != null) {
			gpxSpinner.setSelection(2);
		} else {
			gpxSpinner.setSelection(0);
		}
	}

	@Override
	public void hideControls(FrameLayout layout) {
		removeButton(layout, settingsAppModeButton);
		layout.removeView(settingsAppModeButton);
		mapActivity.accessibleContent.remove(settingsAppModeButton);
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings nightMode) {
		int id = settings.getApplicationMode().getSmallIcon(false); // settingsAppModeButton.isPressed() || dialog != null
		if(cachedId != id && settingsAppModeButton.getLeft() > 0) {
			cachedId = id;
			settingsAppModeButton.setImageResource(id);
		}
	}
}