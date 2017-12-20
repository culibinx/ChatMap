package su.idev.chatmap;

// TODO
/*
may be camera auto upload
*/

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.DatePickerDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RoundRectShape;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.Manifest;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnCameraIdleListener;
import com.google.android.gms.maps.GoogleMap.OnCameraMoveListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowLongClickListener;

import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;


import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;
import com.google.android.gms.maps.model.VisibleRegion;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import com.stfalcon.chatkit.commons.ImageLoader;
import com.stfalcon.chatkit.messages.MessageHolders;
import com.stfalcon.chatkit.messages.MessageInput;
import com.stfalcon.chatkit.messages.MessagesList;
import com.stfalcon.chatkit.messages.MessagesListAdapter;

import android.view.View.OnClickListener;
import android.view.View;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import su.idev.view.IncomingTextMessageViewHolder;
import su.idev.view.OutcomingTextMessageViewHolder;
import su.idev.model.Message;
import su.idev.model.User;
import su.idev.utils.FileUtils;
import su.idev.utils.Identicon;
import su.idev.utils.PermissionUtils;

import static android.app.Notification.PRIORITY_HIGH;
import static android.app.PendingIntent.FLAG_CANCEL_CURRENT;
import static su.idev.utils.FileUtils.BitmapFromURL;
import static su.idev.utils.FileUtils.animateVisibility;
import static su.idev.utils.FileUtils.capitalizePhrase;
import static su.idev.utils.FileUtils.getDeviceName;

public class MapsActivity extends FragmentActivity implements
        MessageInput.InputListener,
        MessageInput.AttachmentsListener,
        MessagesListAdapter.OnLoadMoreListener,
        MessagesListAdapter.OnMessageLongClickListener<Message>,
        OnMapClickListener,
        OnMapLongClickListener,
        OnMarkerClickListener,
        OnCameraIdleListener,
        OnCameraMoveListener,
        OnInfoWindowLongClickListener,
        GoogleMap.InfoWindowAdapter,
        OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback,
        GoogleMap.OnMyLocationChangeListener {

    private static final String TAG = "MapActivity";

    /* AUTH **************************************************************************************/

    private void setDisplayName()
    {
        //
        setDisplayName(false);
    }
    private void setDisplayName(boolean reauth)
    {
        syncSettings(this);
        if (!reauth && displayName != null && displayName.length() > 0) {
            authWithDisplayName();
            return;
        }
        newSyncPathPoint = null;

        if (authDialog != null) {
            if (authDialog.isShowing())
                authDialog.cancel();
            authDialog = null;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.display_name_message)
                .setTitle(R.string.display_auth_title);

        LinearLayout authView = new LinearLayout(MapsActivity.this);
        LinearLayout.LayoutParams authViewParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        authView.setOrientation(LinearLayout.VERTICAL);
        authView.setPadding(20,0,20,0);
        authView.setLayoutParams(authViewParams);

        // user input
        final EditText userInput = new EditText(this);
        userInput.setInputType(InputType.TYPE_CLASS_TEXT);
        userInput.setLines(1);
        userInput.setMaxLines(1);
        userInput.setEllipsize(TextUtils.TruncateAt.END);
        if (displayName != null) {
            userInput.setText(displayName);
        }
        authView.addView(userInput);

        // avatar
        final EditText avatarInput = new EditText(this);
        avatarInput.setInputType(InputType.TYPE_CLASS_TEXT);
        avatarInput.setLines(1);
        avatarInput.setMaxLines(1);
        avatarInput.setEllipsize(TextUtils.TruncateAt.END);
        avatarInput.setHint(R.string.avatar_email);
        avatarInput.setText(avatarName != null && avatarName.length() > 0 ? avatarName : "");
        authView.addView(avatarInput);

        // point notification
        final Switch switchPointNotification = new Switch(this);
        switchPointNotification.setText(R.string.enable_point_notifications);
        switchPointNotification.setChecked(onNotificationPoint);
        authView.addView(switchPointNotification);

        // room notification
        final Switch switchRoomNotification = new Switch(this);
        switchRoomNotification.setText(R.string.enable_room_notifications);
        switchRoomNotification.setChecked(onNotificationRoom);
        authView.addView(switchRoomNotification);

        // path point
        final EditText pathPointInput = new EditText(this);
        pathPointInput.setInputType(InputType.TYPE_CLASS_TEXT);
        pathPointInput.setLines(1);
        pathPointInput.setMaxLines(1);
        pathPointInput.setEllipsize(TextUtils.TruncateAt.END);
        pathPointInput.setHint(R.string.display_path_point);
        pathPointInput.setText(syncPathPoint != null && syncPathPoint.length() > 0 ? syncPathPoint : "public");
        authView.addView(pathPointInput);

        // set view
        builder.setView(authView);

        // add the buttons
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
                boolean ok = true;
                String name = userInput.getText().toString().trim();
                String channel = pathPointInput.getText().toString().replaceAll("[^a-zA-Z0-9_\\-]", "").trim();
                if (channel.length() == 0) {
                    channel = "public";
                }
                if (name.length() == 0) {
                    toast(R.string.display_name_message);
                    ok = false;
                }
                if (!ok) {
                    changeAppState(AppState.ON_SELECT_MARKER);
                } else {
                    newSyncPathPoint = channel;
                    displayName = name;
                    onNotificationPoint = switchPointNotification.isChecked();
                    onNotificationRoom = switchRoomNotification.isChecked();
                    avatarName = avatarInput.getText().toString();
                    authWithDisplayName();
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                boolean ok = true;
                if (displayName == null) {
                    toast(R.string.display_name_message);
                    ok = false;
                }
                String channel = pathPointInput.getText().toString().replaceAll("[^a-zA-Z0-9_\\-]", "").trim();
                if (ok && channel.length() == 0) {
                    toast(R.string.display_path_message);
                    ok = false;
                }
                if (!ok) {
                    if (appState == AppState.ON_CHAT_SHOW) {
                        changeAppState(AppState.ON_SELECT_MARKER);
                    }
                }
            }
        });
        if (currentPointInRoom != null && currentRoomOnPoint != null &&
                appState == AppState.ON_CHAT_SHOW) {
            builder.setNeutralButton(R.string.leave, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    leaveRoom(currentPointInRoom, currentRoomOnPoint);
                    changeAppState(AppState.ON_SELECT_MARKER);
                }
            });
        }
        authDialog = builder.create();
        authDialog.show();
    }

    private void authWithDisplayName()
    {
        if (mAuth == null) { mAuth = FirebaseAuth.getInstance(); }
        if (mAuth != null) {
            if (mAuth.getCurrentUser() != null) {
                authStatus(true, null);
            } else {
                mAuth.signInAnonymously()
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful() && mAuth != null && mAuth.getCurrentUser() != null) {
                                    UserProfileChangeRequest profileUpdates =
                                            new UserProfileChangeRequest.Builder()
                                                    .setDisplayName(displayName).build();
                                    mAuth.getCurrentUser().updateProfile(profileUpdates)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        authStatus(true, null);
                                                    } else {
                                                        sessionID = null;
                                                        authStatus(false, task.getException().toString());
                                                    }
                                                }
                                            });
                                } else {
                                    authStatus(false, task.getException().toString());
                                }
                            }
                        });
            }
        }

    }

    private void authStatus(boolean status, String error)
    {
        if (status) {

            // List all markers for current path
            listenPoints();

            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference sessionRef =
                    database.getReference(SessionRef(UID())).push();
            sessionID = sessionRef.getKey();

            // Queue up a presence operation to remove the session when presence is lost
            queuePresenceOperation(sessionRef, true, null);

            // Register our username in the public user listing.
            DatabaseReference usernameRef =
                    database.getReference(UsersOnlineRef())
                            .child(displayName.toLowerCase())
                            .child(sessionID);
            Map<String, Object> map = new HashMap<>();
            map.put("id", UID());
            map.put("name", displayName);
            queuePresenceOperation(usernameRef, map, null);

            // Monitor connection state so we can requeue disconnect operations if need be.
            if (connectedRef == null) {
                connectedRef = database.getReference(ConnectedRef);
                connectedRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot != null) {
                            for (Map.Entry<String, Object> entry : presence_bits.entrySet()) {
                                String path = entry.getKey();
                                HashMap<String,Object> map = (HashMap<String,Object>)entry.getValue();
                                DatabaseReference ref = (DatabaseReference)map.get("ref");
                                Object onlineValue = map.get("onlineValue");
                                Object offlineValue = map.get("offlineValue");
                                ref.onDisconnect().setValue(offlineValue);
                                ref.setValue(onlineValue);
                            }
                        }
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {}
                });
            }

            // Update messagesAdapter
            toast(getString(R.string.your_nickname) + displayName);

            if (appState == AppState.ON_APPEND_MARKER) {
                sendPoint();
            } if (appState == AppState.ON_CHAT_SHOW) {
                pointRoom(currentPointOnMarker());
            } else {
                changeAppState(AppState.ON_LEAVE_MARKER);
            }

        } else {

            if (error.indexOf("FirebaseNetworkException") > 0) {
                toast(R.string.offline_mode);
            } else {
                toast(R.string.auth_failed);
                //toast(error);
            }

            if (appState == AppState.ON_CHAT_SHOW)
                changeAppState(AppState.ON_SELECT_MARKER);

        }

        syncSettings(this);

    }

    /* POINTS ************************************************************************************/

    private void listenPoints()
    {
        if (sessionID != null)
            changeAppState(AppState.ON_LEAVE_MARKER);

        FirebaseDatabase database = FirebaseDatabase.getInstance();

        if (pointListener != null) {
            DatabaseReference pointRef = database.getReference(mainPoint());
            pointRef.orderByChild("timestamp").removeEventListener(pointListener);
        }

        if (sessionID != null) {
            DatabaseReference sessionRef =
                    database.getReference(SessionRef(UID())).child(sessionID);
            removePresenceOperation(sessionRef, null);

            DatabaseReference usernameRef =
                    database.getReference(UsersOnlineRef())
                            .child(displayName.toLowerCase())
                            .child(sessionID);
            removePresenceOperation(usernameRef, null);
        }

        for(Map.Entry<String, Object> entry : point_marker.entrySet()) {
            String key = entry.getKey();
            HashMap<String,Object> obj = (HashMap<String,Object>)entry.getValue();
            if (obj != null) {
                HashMap<String,Object> point = (HashMap<String,Object>)obj.get("point");
                if (point != null) {
                    String roomId = (String)point.get("roomId");
                    leaveRoom(roomId, true);
                }
                Marker marker = (Marker)obj.get("marker");
                if (marker != null) {
                    marker.remove();
                }
            }
            //receivedPoints.remove(key);
        }
        //syncReceivedPoints(context);

        point_marker.clear();
        mMap.clear();
        initAdapter();

        // Switch to new path
        if (newSyncPathPoint != null) {
            syncPathPoint = newSyncPathPoint;
            newSyncPathPoint = null;
        }

        toast(getString(R.string.current_channel) + syncPathPoint);

        DatabaseReference pointRef = database.getReference(mainPoint());
        pointListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                addPoint(dataSnapshot);
            }
            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                String pointId = dataSnapshot.getKey();
                try {
                    HashMap<String,Object> point = (HashMap<String,Object>)dataSnapshot.getValue();
                    String roomId = (String)point.get("roomId");
                    removePoint(pointId, roomId);
                } catch (Exception ex) {
                    removePoint(pointId, null);
                }
            }
            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                try {
                    String pointId = dataSnapshot.getKey();
                    HashMap<String,Object> pm = (HashMap<String,Object>)point_marker.get(pointId);
                    if (pm != null) {
                        HashMap<String,Object> point = (HashMap<String,Object>)pm.get("point");
                        HashMap<String,Object> point_new = (HashMap<String,Object>)dataSnapshot.getValue();
                        if (point != null && point_new != null) {
                            String roomId = (String)point_new.get("roomId");
                            if (roomId != null) {
                                // update point_marker
                                point.put("roomId", roomId);
                                pm.put("point", point);
                                point_marker.put(pointId, pm);
                            }
                        }
                    }
                } catch (Exception ex) { }
            }
            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        };
        pointRef.orderByChild("timestamp").addChildEventListener(pointListener);
    }

    private void sendPoint() {
        if (sessionID == null) {
            hideSoftKeyboard();
            setDisplayName();
            return;
        }

        String string = mMarkerEdit.getText().toString();
        if (string.length() > 0) {
            String roomId = createRoom(string, false);
            if (roomId != null && roomId.length() > 0) {
                Map<String, Object> data = new HashMap<>();
                data.put("lat", currentCoordinate.latitude);
                data.put("lng", currentCoordinate.longitude);
                data.put("sender", UID());
                data.put("state", string);
                //data.put("ttl", currentTTL);
                data.put("roomId", roomId);
                //data.put("timestamp", new Date().getTime());
                data.put("timestamp", ServerValue.TIMESTAMP);
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                DatabaseReference newClickRef = database.getReference(mainPoint()).push();
                if (newClickRef.getKey() != null) {
                    newClickRef.setValue(data);
                    changeAppState(AppState.ON_LEAVE_MARKER);
                    if (onNotificationPoint) {
                        playSound("send_point");
                    }
                } else {
                    toast(R.string.add_failed);
                }
            } else {
                toast(R.string.add_failed);
            }
        } else {
            toast(R.string.no_data);
        }
    }

    private void addPoint(final DataSnapshot dataSnapshot)
    {
        if (dataSnapshot != null) {
            try {
                final String pointId = dataSnapshot.getKey();
                HashMap<String,Object> point = (HashMap<String,Object>)dataSnapshot.getValue();

                //long elapsed = new Date().getTime() - tryParseLong(point.get("timestamp"), new Date().getTime());
                //long expirySeconds = Math.max(60 * tryParseInt(point.get("ttl"), 1) * 1000 - elapsed, 0);

                boolean isOwnPoint = UID().equals(point.get("sender"));
                String title = (String)point.get("state");
                final String roomId = (String)point.get("roomId");
                //if (expirySeconds > 0) {
                    String custom_type = scanMarkerCustomType(title);
                    LatLng position = new LatLng((double)point.get("lat"), (double)point.get("lng"));
                    Marker marker = mMap.addMarker(
                            new MarkerOptions()
                                    .position(position)
                                    .title(title)
                                    .icon(BitmapDescriptorFactory
                                            .fromBitmap(getMarkerBitmapFromView(MapsActivity.this,custom_type)))
                                    .snippet(pointId));
                    HashMap<String,Object> obj = new HashMap<>();
                    obj.put("marker", marker);
                    obj.put("point", point);
                    point_marker.put(pointId, obj);

                    if (onNotificationPoint &&
                            receivedPoints.get(pointId) == null &&
                            !isOwnPoint) {
                        if (onForeground) {
                            playSound("receive_point");
                        } else {
                            makeNotification(title);
                        }
                    }

                    String _roomId = receivedPoints.get(pointId);
                    if (_roomId != null && _roomId.length() > 0) {
                        enterRoom(pointId, _roomId, title, false);
                    } else {
                        receivedPoints.put(pointId, "");
                        syncReceivedPoints(MapsActivity.this);
                    }
                //} else {
                //    HashMap<String,Object> obj = new HashMap<>();
                //    obj.put("point", point);
                //    point_marker.put(pointId, obj);
                //}

                // Delete on expirySeconds
                //new Handler().postDelayed(new Runnable() {
                //    @Override
                //    public void run() {
                //        deletePoint(pointId,roomId);
                //    }
                //}, expirySeconds);

            } catch (Exception ex) { }
        }
    }

    private void dialogTransferPoint()
    {
        if (dialogTransferPoint == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.display_copy_marker_message)
                    .setTitle(R.string.display_marker_title);

            LinearLayout transferView = new LinearLayout(MapsActivity.this);
            LinearLayout.LayoutParams authViewParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            transferView.setOrientation(LinearLayout.VERTICAL);
            transferView.setPadding(20,0,20,0);
            transferView.setLayoutParams(authViewParams);

            // user input
            final EditText pathPointInput = new EditText(this);
            pathPointInput.setInputType(InputType.TYPE_CLASS_TEXT);
            pathPointInput.setLines(1);
            pathPointInput.setMaxLines(1);
            pathPointInput.setEllipsize(TextUtils.TruncateAt.END);
            pathPointInput.setHint(R.string.display_path_point);
            transferView.addView(pathPointInput);

            builder.setView(transferView);

            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User clicked OK button
                    String channel = pathPointInput.getText().toString().replaceAll("[^a-zA-Z0-9_\\-]", "").trim();
                    transferPoint(channel);
                }
            });
            if (checkPermissionOnDelete()) {
                builder.setNegativeButton(R.string.delete, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked OK button
                        deletePoint(null, null);
                    }
                });
                builder.setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
            } else {
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
            }

            dialogTransferPoint = builder.create();
        }
        dialogTransferPoint.show();
    }

    private void deletePoint(String pointId, String roomId)
    {
        final String _pointId = pointId != null ? pointId : currentPointOnMarker();
        if (_pointId != null) {
            final String _roomId = roomId != null ?  roomId : _pointId.equals(currentPointInRoom) ? currentRoomOnPoint : null;
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            database.getReference(mainPoint()).child(_pointId).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (!task.isSuccessful()) {
                        removePoint(_pointId, _roomId);
                    }
                }
            });
        }
    }

    private void transferPoint(String pathPoint)
    {
        String string = mMarkerView.getText().toString();
        if (currentMarker != null && pathPoint != null &&
                pathPoint.length() > 0 && string.length() > 0) {
            if (pathPoint.equals(syncPathPoint)) {
                toast(R.string.select_other_channel);
            } else {
                String roomId = createRoom(string, false, pathPoint);
                if (roomId != null && roomId.length() > 0) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("lat", currentMarker.getPosition().latitude);
                    data.put("lng", currentMarker.getPosition().longitude);
                    data.put("sender", UID());
                    data.put("state", string);
                    //data.put("ttl", currentTTL);
                    data.put("roomId", roomId);
                    //data.put("timestamp", new Date().getTime());
                    data.put("timestamp", ServerValue.TIMESTAMP);
                    FirebaseDatabase database = FirebaseDatabase.getInstance();
                    DatabaseReference newClickRef = database.getReference(mainPoint(pathPoint)).push();
                    if (newClickRef.getKey() != null) {
                        newClickRef.setValue(data);
                        toast(R.string.ok);
                    } else {
                        toast(R.string.add_failed);
                    }
                } else {
                    toast(R.string.add_failed);
                }
            }
        } else {
            toast(R.string.no_data);
        }
    }

    private void removePoint(String pointId, String roomId)
    {
        if (pointId != null) {
            try {
                String _roomId = roomId;
                boolean silentRemove = false;
                HashMap<String,Object> obj = (HashMap<String,Object>)point_marker.get(pointId);
                if (obj != null) {
                    Marker marker = (Marker)obj.get("marker");
                    if (marker != null) {
                        marker.remove();
                        marker = null;
                    } else {
                        silentRemove = true;
                    }
                    if (_roomId == null) {
                        HashMap<String,Object> point = (HashMap<String,Object>)obj.get("point");
                        if (point != null) {
                            _roomId = (String)point.get("roomId");
                        }
                    }
                    point_marker.remove(pointId);
                }

                if (_roomId != null) {
                    room_del_point.put(_roomId, pointId);
                    leaveRoom(_roomId);
                    roomTimestamp.remove(_roomId);
                    syncRoomTimestamp(MapsActivity.this);
                }

                if (pointId.equals(currentPointOnMarker()) || pointId.equals(currentPointInRoom)) {
                    // Hide dialogs
                    if (dialogTransferPoint != null && dialogTransferPoint.isShowing())
                        dialogTransferPoint.cancel();
                    if (authDialog != null && authDialog.isShowing())
                        authDialog.cancel();

                    // Delete current marker
                    if (currentMarker != null)
                    {
                        currentMarker.remove();
                        currentMarker = null;
                        initAdapter();
                    }

                    // Change state
                    changeAppState(AppState.ON_LEAVE_MARKER);
                }

                if (onForeground && onNotificationPoint) {
                    if (!silentRemove) {
                        playSound("delete_point");
                    }
                    makeNotification(null);
                }

                receivedPoints.remove(pointId);
                syncReceivedPoints(MapsActivity.this);


            } catch (Exception ex) {
                //toast(ex.getMessage());
            }
        }
    }

    private void pointRoom(final String pointId)
    {

        if (sessionID == null || pointId == null) {
            return;
        } else {
            if (pointId.equals(currentPointInRoom)) {
                //toast("already logged room");
                clearUnreaded(currentPointInRoom, currentRoomOnPoint);
                return;
            }
        }

        initAdapter();

        HashMap<String,Object> pm = (HashMap<String,Object>)point_marker.get(pointId);
        if (pm != null) {
            HashMap<String,Object> point = (HashMap<String,Object>)pm.get("point");
            if (point != null) {
                String roomId = (String)point.get("roomId");
                String roomName = (String)point.get("state");
                if (roomName == null) {
                    roomName = "";
                }
                if (roomId != null) {
                    enterRoom(pointId, roomId, roomName, false);
                } else {
                    roomId = createRoom(roomName, false);
                    // update point_marker
                    point.put("roomId", roomId);
                    pm.put("point", point);
                    point_marker.put(pointId, pm);
                    // update ref
                    FirebaseDatabase.getInstance()
                            .getReference(mainPoint())
                            .child(pointId)
                            .child("roomId")
                            .setValue(roomId);
                    enterRoom(pointId, roomId, roomName, false);
                }
                currentRoomOnPoint = roomId;
                currentPointInRoom = pointId;
            }
        }

    }

    /* ROOM **************************************************************************************/

    private String createRoom(final String roomName, final boolean _private)
    {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference newRoomRef = database.getReference(RoomRef()).push();
        final String id = newRoomRef.getKey();
        if (id != null) {
            //toast("createRoom: " + id);
            Map<String, Object> map = new HashMap<>();
            map.put("id", id);
            map.put("name", roomName);
            map.put("type", _private ? "private" : "public");
            map.put("createdByUserId", UID());
            map.put("createdAt", ServerValue.TIMESTAMP);
            if (_private) {
                Map<String, Object> authorizedUsers = new HashMap<>();
                authorizedUsers.put(UID(), true);
                map.put("authorizedUsers", authorizedUsers);
            }
            newRoomRef.setValue(map);
        }
        return id;
    }

    private String createRoom(final String roomName, final boolean _private, final String pathPoint)
    {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference newRoomRef = database.getReference(RoomRef(pathPoint)).push();
        final String id = newRoomRef.getKey();
        if (id != null) {
            //toast("createRoom: " + id);
            Map<String, Object> map = new HashMap<>();
            map.put("id", id);
            map.put("name", roomName);
            map.put("type", _private ? "private" : "public");
            map.put("createdByUserId", UID());
            map.put("createdAt", ServerValue.TIMESTAMP);
            if (_private) {
                Map<String, Object> authorizedUsers = new HashMap<>();
                authorizedUsers.put(UID(), true);
                map.put("authorizedUsers", authorizedUsers);
            }
            newRoomRef.setValue(map);
        }
        return id;
    }

    private void enterRoom(final String pointId, final String roomId, final String roomName,
                           final boolean _private)
    {
        if (pointId != null && pointId.length() > 0 && roomId != null && roomId.length() > 0) {
            // Reload room if exist
            if (room_event.get(roomId) != null) {
                leaveRoom(roomId);
            }

            // Save entering this room
            receivedPoints.put(pointId, roomId);
            syncReceivedPoints(MapsActivity.this);
            //
            if (roomTimestamp.get(roomId) == null || roomId.equals(currentRoomOnPoint)) {
                roomTimestamp.put(roomId, "" + new Date().getTime());
                syncRoomTimestamp(MapsActivity.this);
            }
            //
            updateUnreaded(pointId, roomId);

            final FirebaseDatabase database = FirebaseDatabase.getInstance();

            // Save entering this room to resume the session again later.
            DatabaseReference userRoomRef =
                    database.getReference( UserRoomRef(UID(), roomId) );
            Map<String, Object> map = new HashMap<>();
            map.put("id", roomId);
            map.put("name", roomName);
            map.put("active", true);
            userRoomRef.setValue(map);

            // Set presence bit for the room and queue it for removal on disconnect.
            if (sessionID != null) {
                DatabaseReference presenceRef =
                        database.getReference( UserPresenceRef(roomId, UID(), sessionID) );
                Map<String, Object> onlineValue = new HashMap<>();
                onlineValue.put("id", UID());
                onlineValue.put("name", displayName);
                queuePresenceOperation(presenceRef, onlineValue, null);
            }

            // Setup listeners
            ChildEventListener el = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    onNewMessage(pointId, roomId, dataSnapshot);
                }
                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                    onRemoveMessage(roomId, dataSnapshot);
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {
                    leaveRoom(roomId);
                }
                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
            };
            database.getReference(MessageRef()).child(roomId)
                    .orderByChild("timestamp")
                    .addChildEventListener(el);
            room_event.put(roomId, el);

        }
    }

    private void onNewMessage(String pointId, String roomId, DataSnapshot dataSnapshot)
    {
        HashMap<String,Object> map = (HashMap<String,Object>)dataSnapshot.getValue();
        if (pointId != null && roomId != null && map != null) {
            String id = dataSnapshot.getKey();
            boolean isActiveRoom = roomId.equals(currentRoomOnPoint);
            boolean isOwnMessage = UID().equals(map.get("userId"));
            Long room_ts = tryParseLong(roomTimestamp.get(roomId), 0L);
            Long message_ts = tryParseLong(map.get("timestamp"), new Date().getTime());
            if (isActiveRoom) {
                Message message = getMessage(id, map);
                messagesAdapter.addToStart(message, true);
            }
            if (message_ts > room_ts) {
                if (onNotificationRoom && !isOwnMessage) {
                    updateUnreaded(pointId, roomId);
                    if (onForeground) {
                        if (isActiveRoom) {
                            playSound("receive_message_in");
                        } else {
                            playSound("receive_message_out");
                        }
                    } else {
                        messageToNotification(id, map);
                    }
                }
            }
        }
    }

    private void onRemoveMessage(final String roomId, DataSnapshot dataSnapshot)
    {
        if (roomId != null && roomId.equals(currentRoomOnPoint)) {
            //toast("onRemoveMessage");
            String id = dataSnapshot.getKey();
            messagesAdapter.deleteById(id);
        }
    }

    private void leaveRoom(final String roomId)
    {
        leaveRoom(roomId, false);
    }
    private void leaveRoom(final String roomId, boolean saveState)
    {
        //toast("leaveRoom");
        if (roomId != null) {

            final FirebaseDatabase database = FirebaseDatabase.getInstance();

            // Remove listener for new messages to this room.
            if (room_event.get(roomId) != null) {
                final ChildEventListener child = room_event.get(roomId);
                if (child != null) {
                    database.getReference(MessageRef()).child(roomId).removeEventListener(child);
                }
                room_event.remove(roomId);
            }

            // Remove presence bit for the room and cancel on-disconnect removal.
            if (sessionID != null) {
                DatabaseReference presenceRef =
                        database.getReference( UserPresenceRef(roomId, UID(), sessionID) );
                removePresenceOperation(presenceRef, null);
            }

            // Remove session bit for the room.
            database.getReference( UserRoomRef(UID(), roomId) ).removeValue();

            // Remove chat and remove messages.
            if (room_del_point.get(roomId) != null) {
                database.getReference(MessageRef()).child(roomId).removeValue();
                database.getReference(RoomRef()).child(roomId).removeValue();
                room_del_point.remove(roomId);
            }

            if (currentPointInRoom != null &&
                    currentPointInRoom.equals(currentPointOnMarker())) {
                currentPointInRoom = null;
                currentRoomOnPoint = null;
            }

            makeNotification(null);

            room_unreaded.remove(roomId);

            if (!saveState && roomTimestamp.get(roomId) != null) {
                roomTimestamp.put(roomId, "" + new Date().getTime());
                syncRoomTimestamp(MapsActivity.this);
            }

        }
    }

    private void leaveRoom(String pointId, String roomId)
    {
        if (pointId != null && roomId != null) {
            room_unreaded.put(roomId, -2);
            updateUnreaded(pointId, roomId);
            leaveRoom(roomId);
            //receivedPoints.remove(pointId);
            //syncReceivedPoints(context);
        }
    }

    private void queuePresenceOperation(DatabaseReference ref, Object onlineValue, Object offlineValue)
    {
        String path = ref.toString();
        ref.onDisconnect().setValue(offlineValue);
        ref.setValue(onlineValue);
        HashMap<String,Object> map = new HashMap<>();
        map.put("ref", ref);
        map.put("onlineValue", onlineValue);
        map.put("offlineValue", offlineValue);
        presence_bits.put(path, map);
    }

    private void removePresenceOperation(DatabaseReference ref, Object value)
    {
        String path = ref.toString();
        ref.onDisconnect().cancel();
        ref.setValue(value);
        presence_bits.remove(path);
    }

    private Message getMessage(String id, HashMap<String,Object> map)
    {
        String senderId = (String)map.get("userId");
        String senderName = (String)map.get("name");
        if (senderName == null) { senderName = "noname"; }
        String avatar = (String)map.get("avatar");
        if (avatar == null || avatar.length() == 0) { avatar = senderName; }
        String content = (String)map.get("message");
        long timestamp = tryParseLong(map.get("timestamp"),new Date().getTime());
        String type = (String)map.get("type"); // default
        User user = new User(senderId, senderName, avatar, true);
        Date createdAt = new Date(timestamp);
        Message message = new Message(MapsActivity.this, id, user, content, createdAt);
        return message;
    }

    private void messageToNotification(String id, HashMap<String,Object> map)
    {
        String senderName = (String)map.get("name");
        if (senderName == null) { senderName = "noname"; }
        String text = (String)map.get("message");
        if (text == null) text = "";
        String formatted = senderName + ": " + text;
        makeNotification(formatted);
    }

    private void updateUnreaded(String pointId, String roomId)
    {
        if (roomId != null) {
            Integer unreaded = room_unreaded.get(roomId);
            if (unreaded == null) {
                unreaded = -1;
            }
            unreaded++;
            room_unreaded.put(roomId, unreaded);
            if (pointId != null) {
                HashMap<String,Object> pm = (HashMap<String,Object>)point_marker.get(pointId);
                if (pm != null) {
                    HashMap<String, Object> point = (HashMap<String, Object>) pm.get("point");
                    Marker marker = (Marker)pm.get("marker");
                    if (point != null && marker != null) {
                        String title = (String)point.get("state");
                        String custom_type = scanMarkerCustomType(title);
                        marker.setIcon(BitmapDescriptorFactory
                                .fromBitmap(getMarkerBitmapFromView(MapsActivity.this,custom_type, unreaded)));
                    }
                }
            }
        }
    }
    private void clearUnreaded(String pointId, String roomId)
    {
        if (pointId != null && roomId != null && room_unreaded.get(roomId) != null) {
            room_unreaded.remove(roomId);
            if (roomTimestamp.get(roomId) == null || roomId.equals(currentRoomOnPoint)) {
                roomTimestamp.put(roomId, "" + new Date().getTime());
                syncRoomTimestamp(MapsActivity.this);
            }
            updateUnreaded(pointId, roomId);
        }
    }

    /* MESSAGES ***********************************************************************************/

    @Override
    public void onLoadMore(int page, int totalItemsCount)
    {
        //
    }

    @Override
    public void onMessageLongClick(Message message)
    {
        //toast("bla-bla");
        
    }

    @Override
    public boolean onSubmit(CharSequence input)
    {
        String pointId = currentPointInRoom;
        String roomId = currentRoomOnPoint;
        if (pointId != null && roomId != null) {
            HashMap<String,Object> map = new HashMap<>();
            map.put("userId", UID());
            map.put("name", displayName);
            map.put("avatar", avatarName);
            map.put("message", input.toString());
            map.put("type", "default");
            map.put("timestamp", ServerValue.TIMESTAMP);
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference messageRef = database.getReference(MessageRef()).child(roomId).push();
            messageRef.setValue(map);
            if (onNotificationRoom) {
                playSound("send");
            }
        }
        return true;
    }

    @Override
    public void onAddAttachments()
    {
        Intent intent = new Intent().setClass(MapsActivity.this, ImageSelectActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        intent.putExtra("uid", UID());
        MapsActivity.this.startActivity(intent);
    }

    private void initAdapter()
    {
        if (messagesAdapter != null) {
            messagesAdapter.clear();
            messagesList.removeAllViews();
            messagesAdapter = null;
        }

        MessageHolders holdersConfig = new MessageHolders()
                .setIncomingTextConfig(
                        IncomingTextMessageViewHolder.class,
                        R.layout.item_incoming_text_message)
                .setOutcomingTextConfig(
                        OutcomingTextMessageViewHolder.class,
                        R.layout.item_outcoming_text_message);

        messagesAdapter = new MessagesListAdapter<>(UID(), holdersConfig, imageLoader);
        messagesAdapter.setOnMessageLongClickListener(this);
        messagesList.setAdapter(messagesAdapter);
    }

    /* MARKER *************************************************************************************/

    @Override
    public boolean onMarkerClick(final Marker marker)
    {
        if (currentMarker == null || !marker.equals(currentMarker)) {
            currentMarker = marker;
            if (currentMarker.getSnippet().equals("find_address")) {
                changeAppState(AppState.ON_SELECT_FIND_ADDRESS_MARKER);
            } else {
                changeAppState(AppState.ON_SELECT_MARKER);
            }
        }
        return true;
    }

    /* INFO **************************************************************************************/

    @Override
    public void onInfoWindowLongClick(Marker marker)
    {
        String geo = marker.getSnippet().equals("find_address")
        ? "geo:" +
            marker.getPosition().latitude + "," + marker.getPosition().longitude +
            "?z=" + mMap.getCameraPosition().zoom +
            "&q=" + Uri.encode(mFindAddressEdit.getText().toString())
        : "geo:0,0?q=" + Uri.encode(
            marker.getPosition().latitude + "," +
            marker.getPosition().longitude + "(" + marker.getTitle() + ")");

        Uri gmmIntentUri = Uri.parse(geo);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        }
    }

    @Override
    public View getInfoWindow(Marker marker)
    {
        if (marker.getSnippet().equals("new_marker")) {
            LinearLayout infoView = new LinearLayout(MapsActivity.this);
            LinearLayout.LayoutParams infoViewParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            infoView.setOrientation(LinearLayout.HORIZONTAL);
            infoView.setLayoutParams(infoViewParams);

            ImageView infoImageView = new ImageView(MapsActivity.this);
            Drawable drawable = getResources().getDrawable(android.R.drawable.ic_dialog_map);
            infoImageView.setImageDrawable(drawable);
            if (currentMapType%2 > 0) {
                infoImageView.setColorFilter(ContextCompat.getColor(MapsActivity.this,R.color.colorPrimary));
            }
            infoView.addView(infoImageView);

            return infoView;
        }
        return null;
    }

    @Override
    public View getInfoContents(Marker marker)
    {
        //
        return prepareInfoView(marker);
    }

    private View prepareInfoView(Marker marker)
    {
        LinearLayout infoView = new LinearLayout(MapsActivity.this);
        LinearLayout.LayoutParams infoViewParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        infoView.setOrientation(LinearLayout.HORIZONTAL);
        infoView.setLayoutParams(infoViewParams);

        LinearLayout subInfoView = new LinearLayout(MapsActivity.this);
        LinearLayout.LayoutParams subInfoViewParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subInfoView.setOrientation(LinearLayout.VERTICAL);
        subInfoView.setLayoutParams(subInfoViewParams);

        TextView subInfo = new TextView(MapsActivity.this);
        subInfo.setText(marker.getTitle());
        subInfoView.addView(subInfo);
        infoView.addView(subInfoView);

        return infoView;
    }

    /* MAP **************************************************************************************/
    @Override
    public void onMyLocationChange(Location location) {
        if (location != null) {
            updateLocation(followMap);
        }
    }

    @Override
    public void onMapClick(final LatLng point)
    {
        changeAppState(AppState.ON_LEAVE_MARKER);
    }

    @Override
    public void onMapLongClick(final LatLng point)
    {
        currentCoordinate = point;
        changeAppState(AppState.ON_APPEND_MARKER);
        changeAppState(AppState.ON_MOVE_APPEND_MARKER);
    }

    @Override
    public void onCameraIdle() {
        //
    }

    @Override
    public void onCameraMove() {
        if (appState == AppState.ON_APPEND_MARKER) {
            changeAppState(AppState.ON_MOVE_APPEND_MARKER);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        mMap = googleMap;
        mMap.setMinZoomPreference(1);
        mMap.setMaxZoomPreference(18);

        reloadTileOverlay();
        settingsClick();

        // Set listeners
        mMap.setOnCameraMoveListener(this);
        mMap.setOnCameraIdleListener(this);

        mMap.setOnMapClickListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnInfoWindowLongClickListener(this);
        mMap.setInfoWindowAdapter(this);
        mMap.setOnMyLocationChangeListener(this);

        enableMyLocation();
    }

    /* MAIN *************************************************************************************/

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);

        try {
            if (this.receiver != null) {
                this.unregisterReceiver(this.receiver);
                this.receiver = null;
            }
        } catch (Exception ex) {
            Log.e(TAG, "BroadcastReceiver exists");
        }
        this.receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    String source = extras.getString("source");
                    String target = extras.getString("target");
                    String body = extras.getString("body");
                    if ("MapsActivity".equals(target)) {

                        if ("ImageViewActivity".equals(source)) {
                            needRefresh = true;
                        }
                        if ("ImageSelectActivity".equals(source)) {
                            if (appState == AppState.ON_CHAT_SHOW &&
                                body != null && body.length() > 0) {
                                messagesInput.getInputEditText().append("<" + body + "> ");
                            }
                        }
                        //Log.i(TAG, source + "/" + target + " -> " + body);
                    }
                }
            }
        };
        this.registerReceiver(this.receiver, new IntentFilter(REFRESH_ACTION_FILTER));

        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        setupControls();
        setupMessages();
    }

    private void setupControls()
    {
        mSettingsButton = (ImageButton) findViewById(R.id.settingsButton);
        mSettingsButton.setOnClickListener(new View.OnClickListener()   {
            public void onClick(View v)  {
                settingsClick();
            }
        });
        mSettingsButton.requestFocus();

        mFindAddressEdit = (EditText) findViewById(R.id.findAddressEdit);
        mFindAddressEdit.setImeOptions(EditorInfo.IME_ACTION_DONE);
        mFindAddressEdit.setInputType(InputType.TYPE_CLASS_TEXT);
        mFindAddressEdit.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
            {
                if(actionId == EditorInfo.IME_ACTION_DONE)
                {
                    findAddressClick();
                    return true;
                }
                return false;
            }
        });
        InputFilter filter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                for (int i = start; i < end; i++) {
                    int type = Character.getType(source.charAt(i));
                    if (type == Character.SURROGATE || type == Character.OTHER_SYMBOL) {
                        return "";
                    }
                }
                return null;
            }
        };
        mFindAddressEdit.setFilters(new InputFilter[]{filter});


        mFindAddressButton = (ImageButton) findViewById(R.id.findAddressButton);
        mFindAddressButton.setOnClickListener(new View.OnClickListener()   {
            public void onClick(View v)  {
                findAddressClick();
            }
        });

        pulseView = findViewById(R.id.pulseView);
        pulseView.setAlpha(0);
        //pulseView.setVisibility(View.GONE);

        // Create flash animation
        ShapeDrawable selShape = new ShapeDrawable (new OvalShape());
        //selShape.setIntrinsicWidth (5);
        //selShape.setIntrinsicHeight (5);
        selShape.getPaint().setColor(Color.RED);

        ShapeDrawable defShape = new ShapeDrawable (new OvalShape());
        defShape.getPaint().setColor(Color.WHITE);

        flashAnimation = new AnimationDrawable();
        flashAnimation.addFrame(selShape, 500);
        flashAnimation.addFrame(defShape, 500);
        flashAnimation.setOneShot(false);
        pulseView.setBackground(flashAnimation);

        // Create pulse animation
        pulseAnimation = ObjectAnimator.ofPropertyValuesHolder(pulseView,
                PropertyValuesHolder.ofFloat("scaleX", 1.5f),
                PropertyValuesHolder.ofFloat("scaleY", 1.5f));
        pulseAnimation.setDuration(500);
        pulseAnimation.setRepeatCount(ObjectAnimator.INFINITE);
        pulseAnimation.setRepeatMode(ObjectAnimator.REVERSE);

        // May be overwrite clicks
        /*
        pulseLayout = (LinearLayout) findViewById(R.id.pulseLayout);
        pulseLayout.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN){
                    int x = (int) event.getX();
                    int y = (int) event.getY();
                }
                return MapsActivity.super.onTouchEvent(event);
            }
        });
        */

        mPlusButton = (ImageButton) findViewById(R.id.plusButton);
        mPlusButton.setOnClickListener(new View.OnClickListener()   {
            public void onClick(View v)  {
                plusClick();
            }
        });
        mMinusButton = (ImageButton) findViewById(R.id.minusButton);
        mMinusButton.setOnClickListener(new View.OnClickListener()   {
            public void onClick(View v)  {
                minusClick();
            }
        });
        mLocationButton = (ImageButton) findViewById(R.id.locationButton);
        mLocationButton.setOnClickListener(new View.OnClickListener()   {
            public void onClick(View v)  {
                locationClick();
            }
        });

        /*
        mMarkerExpireButton = (Button) findViewById(R.id.markerExpireButton);
        mMarkerExpireButton.setTransformationMethod(null);
        mMarkerExpireButton.setOnClickListener(new View.OnClickListener()   {
            public void onClick(View v)  {
                markerExpireClick();
            }
        });
        mMarkerExpireButton.setText("00:00:00");
        */

        /*
        mMarkerTTLButton = (Button) findViewById(R.id.markerTTLButton);
        mMarkerTTLButton.setTransformationMethod(null);
        mMarkerTTLButton.setOnClickListener(new View.OnClickListener()   {
            public void onClick(View v)  {
                markerTTLClick();
            }
        });
        mMarkerTTLButton.setText("");
        */

        mMarkerTransferButton = (ImageButton) findViewById(R.id.markerTransferButton);
        mMarkerTransferButton.setOnClickListener(new View.OnClickListener()   {
            public void onClick(View v)  {
                markerTransferClick();
            }
        });

        mChannelSettings = (ImageButton) findViewById(R.id.channelSettings);
        mChannelSettings.setOnClickListener(new View.OnClickListener()   {
            public void onClick(View v)  {
                channelSettingsClick();
            }
        });
        mChatSettings = (ImageButton) findViewById(R.id.chatSettings);
        mChatSettings.setOnClickListener(new View.OnClickListener()   {
            public void onClick(View v)  {
                channelSettingsClick();
            }
        });

        mMarkerView = (TextView) findViewById(R.id.markerView);
        mMarkerView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mMarkerChatView.setText(s);
            }
            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        mMarkerChatView = (TextView) findViewById(R.id.markerChatView);
        mMarkerEdit = (EditText) findViewById(R.id.markerEdit);

        mMarkerSendButton = (ImageButton) findViewById(R.id.markerSendButton);
        mMarkerSendButton.setOnClickListener(new View.OnClickListener()   {
            public void onClick(View v)  {
                markerSendClick();
            }
        });

        mChatLayout = (RelativeLayout) findViewById(R.id.chat);
        mChatLayout.setVisibility(View.GONE);

        mTitleView = (LinearLayout) findViewById(R.id.titleView);
        mTitleView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                changeAppState(AppState.ON_CHAT_SHOW);
            }
        });
        mChatView = (LinearLayout) findViewById(R.id.chatView);
        mChatView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                clearUnreaded(currentPointInRoom, currentRoomOnPoint);
                changeAppState(AppState.ON_SELECT_MARKER);
            }
        });
        mEditView = (LinearLayout) findViewById(R.id.editView);

        changeAppState(AppState.ON_LEAVE_MARKER);

    }

    private void setupMessages()
    {
        // chat
        imageLoader = new ImageLoader() {
            @Override
            public void loadImage(final ImageView imageView, final String url) {
                Bitmap bmd = Identicon.create(url);
                imageView.setImageBitmap(bmd);
                new AsyncTask<Void, Void, Bitmap>() {
                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                    }

                    @Override
                    protected Bitmap doInBackground(Void... params) {
                        return BitmapFromURL(MapsActivity.this, url);
                    }

                    @Override
                    protected void onPostExecute(Bitmap bm) {
                        super.onPostExecute(bm);
                        if (bm != null) {
                            imageView.setImageBitmap(bm);
                        }
                    }
                }.execute();

            }
        };
        messagesList = (MessagesList) findViewById(R.id.messagesList);
        initAdapter();

        messagesInput = (MessageInput) findViewById(R.id.messagesInput);
        messagesInput.setInputListener(this);
        messagesInput.setAttachmentsListener(this);

    }

    private void settingsClick()
    {
        if (currentMapType > 3) {
            currentMapType = 0;
        } else {
            currentMapType++;
        }
        switch (currentMapType) {
            case 1:
                mSettingsButton.setImageResource(R.drawable.ic_map);
                break;
            case 2:
                mSettingsButton.setImageResource(R.drawable.ic_sattelite);
                break;
            case 3:
                mSettingsButton.setImageResource(R.drawable.ic_terrain);
                break;
            case 4:
                mSettingsButton.setImageResource(R.drawable.ic_sattelite);
                break;
            default:
                mSettingsButton.setImageResource(R.drawable.ic_offline);
                break;

        }
        if ((currentMapType == 1) || (currentMapType == 3)) {
            mFindAddressEdit.setTextColor(getResources().getColor(R.color.black));
        } else {
            mFindAddressEdit.setTextColor(getResources().getColor(R.color.gray_light));
        }
        reloadTileOverlay();
        if (mTileOverlay != null) {
            mTileOverlay.setTransparency(currentMapType == 0 ? 0f : 1f);
        }
        mMap.setMapType(currentMapType);

    }

    private void findAddressClick()
    {
        String string = mFindAddressEdit.getText().toString();
        if (string.length() < 1) { toast(R.string.no_data); return; }
        mFindAddressButton.requestFocus();

        if (appState != AppState.ON_LEAVE_MARKER)
            changeAppState(AppState.ON_LEAVE_MARKER);
        else
            hideSoftKeyboard();

        Geocoder geoCoder = new Geocoder(this, Locale.getDefault());
        try
        {
            List<Address> addresses = geoCoder.getFromLocationName(string, 5);
            if (addresses.size() > 0)
            {
                double lat = addresses.get(0).getLatitude();
                double lon = addresses.get(0).getLongitude();
                LatLng user = new LatLng(lat, lon);
                onMapClick(user);
                final Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(user)
                        .snippet("find_address")
                        .icon(BitmapDescriptorFactory
                                .fromBitmap(getMarkerBitmapFromView(MapsActivity.this,"\uD83D\uDD0D")))
                        .title(getString(R.string.find_here)));
                marker.showInfoWindow();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        marker.remove();
                    }
                }, 15000);

                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(user, 15));
                //mMap.animateCamera(CameraUpdateFactory.zoomTo(10), 1000, null);

                mFindAddressEdit.setText("");
            } else {
                toast(R.string.no_data);
            }
        }
        catch (Exception e)
        {
            toast(R.string.no_data);
        }
    }

    private void plusClick()
    {
        float zoom = mMap.getCameraPosition().zoom;
        if (zoom < mMap.getMaxZoomLevel()) {
            mMap.animateCamera( CameraUpdateFactory.zoomTo( zoom + 1 ) );
        }

    }
    private void minusClick()
    {
        float zoom = mMap.getCameraPosition().zoom;
        if (zoom > mMap.getMinZoomLevel()) {
            mMap.animateCamera( CameraUpdateFactory.zoomTo( zoom - 1 ) );
        }

    }
    private void locationClick()
    {
        if (!mPermissionLocationDenied) {
            updateLocation(true);
            followMap = !followMap;
            if (followMap) {
                mLocationButton.setImageResource(R.drawable.ic_location);
            } else {
                mLocationButton.setImageResource(R.drawable.ic_location_search);
            }
        } else {
            toast("Unable to fetch the current location");
        }
    }

    private void channelSettingsClick()
    {
        //
        setDisplayName(true);
    }

    private void markerTransferClick()
    {
        if (sessionID != null) {
            dialogTransferPoint();
        }
    }

    /*
    private void markerTTLClick()
    {
        showTTLSelect();
    }
    */

    private void markerSendClick()
    {
        //
        sendPoint();
    }

    /*
    private void showTTLSelect() {
        final Calendar currentDate = Calendar.getInstance();
        currentDateTTL = Calendar.getInstance();
        //dateTTL.add(Calendar.MINUTE, currentTTL);
        DatePickerDialog dateDialog = new DatePickerDialog(MapsActivity.this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                currentDateTTL.set(year, monthOfYear, dayOfMonth);
                TimePickerDialog timeDialog = new TimePickerDialog(MapsActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        currentDateTTL.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        currentDateTTL.set(Calendar.MINUTE, minute);
                        long elapsed = currentDateTTL.getTimeInMillis() - currentDate.getTimeInMillis();
                        currentTTL = (int)Math.floor(elapsed/60/1000);
                        if (currentTTL < 1) {
                            toast(R.string.min_ttl_data);
                            currentTTL = 1;
                        }
                        changeAppState(AppState.ON_DEFAULT);
                    }
                }, currentDateTTL.get(Calendar.HOUR_OF_DAY), currentDateTTL.get(Calendar.MINUTE), true);
                timeDialog.setTitle(R.string.display_ttl_message);
                timeDialog.show();
            }
        }, currentDateTTL.get(Calendar.YEAR), currentDateTTL.get(Calendar.MONTH), currentDateTTL.get(Calendar.DATE));
        dateDialog.setTitle(R.string.display_ttl_message);
        dateDialog.show();
    }
    */

    private boolean checkPermissionOnDelete()
    {
        if (sessionID == null) {
            setDisplayName();
        }
        // For debug only
        if (getDeviceName().contains("Vertex")) {
            return true;
        }
        if (currentMarker != null) {
            HashMap<String,Object> obj = (HashMap<String,Object>)point_marker.get(currentMarker.getSnippet());
            if (obj != null) {
                HashMap<String,Object> point = (HashMap<String,Object>)obj.get("point");
                if (point != null) {
                    String senderId = (String)point.get("sender");
                    return UID().equals(senderId);
                }
            }
        }
        return false;
    }

    private void reloadTileOverlay()
    {
        if (mTileOverlay != null) {
            mTileOverlay.remove();
            mTileOverlay = null;
        }
        if ((currentMapType == 0) || (currentMapType > 4)) {
            mTileOverlay = mMap.addTileOverlay(new TileOverlayOptions()
                    .tileProvider(new ZTileProvider()).fadeIn(true));
        }
    }

    private void enableMyLocation()
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else  {
            if (mMap != null) {
                if (sessionID == null) { setDisplayName(); }
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                updateLocation(currentLatitude, currentLongitude, true);
            }
        }
    }

    private void updateLocation(boolean followMap)
    {
        if (!mPermissionLocationDenied) {
            Location myLocation  = mMap.getMyLocation();
            if (myLocation != null) {
                currentLatitude = myLocation.getLatitude();
                currentLongitude = myLocation.getLongitude();
            }
            currentZoom = mMap.getCameraPosition().zoom;
            if (followMap) {
                mMap.animateCamera(CameraUpdateFactory
                        .newLatLng(new LatLng(currentLatitude, currentLongitude)));
            }
        } else {
            //enableMyLocation();
        }

    }

    private void updateLocation(double lat, double lon, boolean followMap)
    {
        if (!mPermissionLocationDenied) {
            currentLatitude = lat;
            currentLatitude = lon;
            if (followMap) {
                mMap.animateCamera(CameraUpdateFactory
                        .newLatLng(new LatLng(currentLatitude, currentLongitude)));
            }
        } else {
            //enableMyLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                enableMyLocation();
            } else {
                mPermissionLocationDenied = true;
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

    }

    private void changeAppState(AppState state) {

        // Return on existing and pass on select marker
        if (appState == state && state != AppState.ON_SELECT_MARKER) {
            return;
        }

        switch (state) {
            case ON_LEAVE_MARKER:
            {
                // Disable keyboard
                hideSoftKeyboard();

                if (mMarkerEdit.isFocused()) mMarkerEdit.clearFocus();
                if (mFindAddressEdit.isFocused()) mFindAddressEdit.clearFocus();

                // Enable findAddress
                mFindAddressEdit.setEnabled(true);
                mFindAddressButton.setEnabled(true);
                // Hide info window
                if (currentMarker != null && currentMarker.isInfoWindowShown())
                    currentMarker.hideInfoWindow();

                // Clear focus
                mMarkerEdit.clearFocus();
                mMarkerSendButton.clearFocus();

                // Disable timer
                //if (mTimer != null) { mTimer.cancel(); mTimer = null; }
                //mMarkerExpireButton.setText("");
                // Update message view text
                mMarkerView.setText("");
                // Hide new point
                if (pulseView.getAlpha() == 1) pulseView.setAlpha(0);
                if (flashAnimation.isRunning()) flashAnimation.stop();
                if (pulseAnimation.isStarted()) pulseAnimation.cancel();

                // State panel
                animateVisibility(mTitleView, false);
                animateVisibility(mEditView, false);
                animateVisibility(mChatLayout, false);
                animateVisibility(mChatView, false);

                // Clear currentMarker
                currentMarker = null;

                break;
            }
            case ON_APPEND_MARKER:
            {
                // Disable findAddress
                mFindAddressEdit.setEnabled(false);
                mFindAddressButton.setEnabled(false);
                // Hide info window
                if (currentMarker != null && currentMarker.isInfoWindowShown()) currentMarker.hideInfoWindow();
                // Disable timer
                //if (mTimer != null) { mTimer.cancel(); mTimer = null; }
                //mMarkerTTLButton.setText(formatExpiryTTL(MapsActivity.this, currentTTL));
                // Update message view text
                mMarkerView.setText("");
                // Show new point
                if (currentCoordinate != null) {
                    Point point = mMap.getProjection().toScreenLocation(currentCoordinate);
                    if (pulseView.getAlpha() == 0) pulseView.setAlpha(1);
                    pulseView.setX(point.x); pulseView.setY(point.y);
                    if (!flashAnimation.isRunning()) flashAnimation.start();
                    if (!pulseAnimation.isStarted()) pulseAnimation.start();
                }

                // State panel
                animateVisibility(mTitleView, false);
                animateVisibility(mEditView, true, 0.7f);
                animateVisibility(mChatLayout, false);
                animateVisibility(mChatView, false);


                break;
            }
            case ON_SELECT_MARKER:
            {
                // Disable keyboard
                hideSoftKeyboard();

                // Disable findAddress
                mFindAddressEdit.setEnabled(false);
                mFindAddressButton.setEnabled(false);
                // Show info window
                if (currentMarker != null && !currentMarker.isInfoWindowShown())
                    currentMarker.showInfoWindow();

                // Clear focus
                mMarkerEdit.clearFocus();
                mMarkerSendButton.clearFocus();

                // Re enable timer
                //if (mTimer != null) { mTimer.cancel(); mTimer = null; }
                //if (mExpireTimerTask != null) { mExpireTimerTask.cancel(); mExpireTimerTask = null; }
                //mExpireTimerTask = new ExpireTimerTask();
                //mTimer = new Timer(); mTimer.schedule(mExpireTimerTask, 1000, 1000);
                //mMarkerExpireButton.setText(formatExpiryTime(MapsActivity.this, currentMarker != null ? currentMarker.getSnippet() : null, point_marker));
                // Update message view text
                if (currentMarker != null) mMarkerView.setText(currentMarker.getTitle().replace("\r", "").replace("\n", ""));
                else mMarkerView.setText("");

                //Hide or show delete button
                if (sessionID != null) {
                    mMarkerTransferButton.setVisibility(View.VISIBLE);
                } else {
                    mMarkerTransferButton.setVisibility(View.GONE);
                }

                // Hide new point
                if (pulseView.getAlpha() == 1) pulseView.setAlpha(0);
                if (flashAnimation.isRunning()) flashAnimation.stop();
                if (pulseAnimation.isStarted()) pulseAnimation.cancel();

                // State panel
                animateVisibility(mTitleView, true, 0.7f);
                animateVisibility(mEditView, false);
                animateVisibility(mChatLayout, false);
                animateVisibility(mChatView, false);

                break;
            }
            case ON_SELECT_FIND_ADDRESS_MARKER:
            {
                // Disable keyboard
                hideSoftKeyboard();

                // Disable findAddress
                mFindAddressEdit.setEnabled(false);
                mFindAddressButton.setEnabled(false);
                // Show info window
                if (currentMarker != null && !currentMarker.isInfoWindowShown())
                    currentMarker.showInfoWindow();

                // Clear focus
                mMarkerEdit.clearFocus();
                mMarkerSendButton.clearFocus();

                // Disable timer
                //if (mTimer != null) { mTimer.cancel(); mTimer = null; }
                //mMarkerExpireButton.setText("");
                // Update message view text
                mMarkerView.setText("");
                // Hide new point
                if (pulseView.getAlpha() == 1) pulseView.setAlpha(0);
                if (flashAnimation.isRunning()) flashAnimation.stop();
                if (pulseAnimation.isStarted()) pulseAnimation.cancel();

                // State panel
                animateVisibility(mTitleView, false);
                animateVisibility(mEditView, false);
                animateVisibility(mChatLayout, false);
                animateVisibility(mChatView, false);


                break;
            }
            case ON_CHAT_SHOW:
            {
                // Update message view text
                if (currentMarker != null) mMarkerView.setText(currentMarker.getTitle().replace("\r", "").replace("\n", ""));
                else mMarkerView.setText("");

                // Login into chat
                if (sessionID == null) setDisplayName();
                else pointRoom(currentPointOnMarker());

                // State panel
                animateVisibility(mTitleView, false);
                animateVisibility(mEditView, false);
                animateVisibility(mChatLayout, true);
                animateVisibility(mChatView, true);

                break;
            }
            case ON_MOVE_APPEND_MARKER:
            {
                // Refresh new point
                if (currentCoordinate != null) {
                    Point point = mMap.getProjection().toScreenLocation(currentCoordinate);
                    if (pulseView.getAlpha() == 0) pulseView.setAlpha(1);
                    pulseView.setX(point.x); pulseView.setY(point.y);
                    if (!flashAnimation.isRunning()) flashAnimation.start();
                    if (!pulseAnimation.isStarted()) pulseAnimation.start();
                }
            }
            default:
            {
                if (appState == AppState.ON_APPEND_MARKER) {
                    // Update expire button text
                    //mMarkerTTLButton.setText(formatExpiryTTL(MapsActivity.this, currentTTL));
                } else {
                    // Update message view text
                    if (currentMarker != null) mMarkerView.setText(currentMarker.getTitle().replace("\r", "").replace("\n", ""));
                    else mMarkerView.setText("");
                }

                break;
            }

        }

        if (state != AppState.ON_DEFAULT && state != AppState.ON_MOVE_APPEND_MARKER)
            appState = state;

    }

    /* CONFIGURATION *****************************************************************************/

    @Override
    protected void onStart() {
        super.onStart();
        onForeground = true;
        makeNotification(null);
    }
    @Override
    protected void onStop() {
        super.onStop();
        onForeground = false;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        changeAppState(AppState.ON_DEFAULT);
        /*
        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            toast("landscape");
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            toast("portrait");
        }
        */
    }

    @Override
    protected void onResume() {
        super.onResume();
        onForeground = true;
        if (messagesAdapter != null) {
            if (needRefresh) {
                needRefresh = false;
                messagesAdapter.notifyDataSetChanged();
            }
            //messagesList.removeAllViews();
            //messagesList.setAdapter(messagesAdapter);
        }
    }

    @Override
    public void onBackPressed() {
        if (appState == AppState.ON_CHAT_SHOW) {
            clearUnreaded(currentPointInRoom, currentRoomOnPoint);
            changeAppState(AppState.ON_SELECT_MARKER);
        } else {
            //super.onBackPressed();
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (mPermissionLocationDenied) {
            showMissingPermissionError();
            mPermissionLocationDenied = false;
        }
    }

    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }

    /* OVERLAY ************************************************************************************/

    private class ZTileProvider implements TileProvider {

        private String BASE_URL = "http://mt2.google.com/vt/lyrs=y@176103410&x=%d&y=%d&z=%d&hl=ru&scale=2";
        private File cacheMapDirectory;
        private int TILE_SIZE_DP = 256;

        public ZTileProvider() {
            cacheMapDirectory = FileUtils.getBaseCacheDir(MapsActivity.this,"map");
        }

        @Override
        public Tile getTile(int x, int y, int zoom) {
            int _zoom = zoom > 18 ? 18 : zoom < 1 ? 1 : zoom;
            try {
                String source = String.format(Locale.ENGLISH, BASE_URL, x, y, _zoom);
                File file = new File(cacheMapDirectory, FileUtils.md5(source));
                if (file.exists()) {
                    InputStream inputFile = new FileInputStream(file);
                    byte[] bytes = FileUtils.convertStreamToByteArray(inputFile);
                    inputFile.close();
                    return new Tile(TILE_SIZE_DP, TILE_SIZE_DP, bytes);
                } else {
                    try {
                        // Determine filesize
                        URL url = new URL(source);
                        URLConnection connection = url.openConnection();
                        connection.setConnectTimeout(1000*3);
                        connection.connect();
                        int fileLength = connection.getContentLength();
                        // Stop if filesize is zero
                        if (fileLength < 0)
                            return NO_TILE;
                        InputStream input = new BufferedInputStream(url.openStream(), 8192);
                        OutputStream output = new FileOutputStream(file);
                        byte data[] = new byte[8192];
                        int count = -1;
                        while ((count = input.read(data)) != -1) {
                            //while ((count = input.read(data)) != -1) {
                            output.write(data, 0, count);
                        }
                        // flushing output
                        output.flush();
                        // closing streams
                        output.close();
                        input.close();

                        InputStream inputFile = new FileInputStream(file);
                        byte[] bytes = FileUtils.convertStreamToByteArray(inputFile);
                        inputFile.close();
                        return new Tile(TILE_SIZE_DP, TILE_SIZE_DP, bytes);

                    } catch (Exception e) {
                        return NO_TILE;
                    }
                }
            } catch (Exception ex) {
                return NO_TILE;
            }

        }
    }

    /* UTILS **************************************************************************************/

    public void hideSoftKeyboard() {
        View view = this.getCurrentFocus();
        if (view == null) {
            view = new View(MapsActivity.this);
        }
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        /*
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(),0);
         */
    }

    private boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    private void toast(String text) { Toast.makeText(MapsActivity.this, text, Toast.LENGTH_SHORT).show(); }
    private void toast(int resourceId)
    {
        Toast.makeText(MapsActivity.this, resourceId, Toast.LENGTH_SHORT).show();
    }

    /*
    private class ExpireTimerTask extends TimerTask {

        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (appState == AppState.ON_SELECT_MARKER) {
                        mMarkerExpireButton.setText(formatExpiryTime(MapsActivity.this,
                                currentMarker != null ? currentMarker.getSnippet() : null, point_marker));
                    }
                }
            });
        }
    }
    */

    /*
    public static String formatExpiryTime (Context context, String key,
                                           HashMap<String,Object> point_marker) {
        if (key != null) {
            HashMap<String,Object> obj = (HashMap<String,Object>)point_marker.get(key);
            if (obj != null) {
                HashMap<String,Object> point = (HashMap<String,Object>)obj.get("point");
                if (point != null) {
                    long expirySeconds;
                    try {
                        long timestamp = tryParseLong(point.get("timestamp"),new Date().getTime());
                        int ttl = tryParseInt(point.get("ttl"), 1);
                        long elapsed = new Date().getTime() - timestamp;
                        expirySeconds = Math.max(60 * ttl * 1000 - elapsed, 0);
                    } catch (Exception ex) { expirySeconds = 0; }
                    return timeSpan(context,expirySeconds);
                }
            }
        }
        return "";
    }
    */

    /*
    public static String formatExpiryTTL (Context context, int currentTTL) {
        return timeSpan(context, currentTTL*60*1000);
    }
    */
    /*
    private static String slf(double n, String append) {
        String s = String.valueOf(Double.valueOf(Math.floor(n)).longValue());
        return s.equals("0") ? "" : s + append;
    }
    */
    /*
    private static String timeSpan(Context context, long timeInMs) {
        double t = Double.valueOf(timeInMs);
        if(t < 1000d)
            return slf(t, context.getString(R.string.milliseconds));
        if(t < 60000d)
            return slf(t / 1000d, context.getString(R.string.seconds));
        if(t < 3600000d)
            return slf(t / 60000d, context.getString(R.string.minutes));
                    //+
                    //slf((t % 60000d) / 1000d, getString(R.string.seconds));
                    //+ slf(t % 1000d) + "ms";
        if(t < 86400000d)
            return slf(t / 3600000d, context.getString(R.string.hours));
                    //+
                    //slf((t % 3600000d) / 60000d, getString(R.string.minutes)) +
                    //slf((t % 60000d) / 1000d, getString(R.string.seconds));
                    //+ slf(t % 1000d) + "ms";
        return slf(t / 86400000d, context.getString(R.string.days));
                //+
                //slf((t % 86400000d) / 3600000d, getString(R.string.hours)) +
                //slf((t % 3600000d) / 60000d, getString(R.string.minutes)) +
                //slf((t % 60000d) / 1000d, getString(R.string.seconds));
                //+ slf(t % 1000d) + "ms";
    }
    */
    private static Bitmap getMarkerBitmapFromView(Context context, String custom_type)
    {
        //
        return getMarkerBitmapFromView(context, custom_type, -1);
    }

    private static Bitmap getMarkerBitmapFromView(Context context, String custom_type, Integer unreaded)
    {

        RelativeLayout markerView = new RelativeLayout(context);
        LinearLayout.LayoutParams markerViewParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        markerView.setLayoutParams(markerViewParams);

        TextView markerTextView = new TextView(context);
        markerTextView.setTextSize(24);
        markerTextView.setText(custom_type);
        markerTextView.setBackground(context.getDrawable(
                unreaded >= 0 ? R.drawable.rounded_marker_room : R.drawable.rounded_marker));
        markerView.addView(markerTextView);

        TextView unreadedTextView = new TextView(context);
        unreadedTextView.setTextColor(context.getResources().getColor(R.color.colorRed));
        unreadedTextView.layout(0,0,0,0);
        unreadedTextView.setTextSize(10);
        unreadedTextView.setText("" + unreaded);
        unreadedTextView.setBackground(context.getDrawable(R.drawable.rounded_unreaded));
        unreadedTextView.setAlpha(unreaded > 0 ? 1 : 0);
        markerView.addView(unreadedTextView);

        markerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        markerView.layout(0, 0, markerView.getMeasuredWidth(), markerView.getMeasuredHeight());
        markerView.buildDrawingCache();
        Bitmap returnedBitmap = Bitmap.createBitmap(markerView.getMeasuredWidth(), markerView.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);
        canvas.drawColor(Color.WHITE, PorterDuff.Mode.SRC_IN);

        Drawable drawable = markerView.getBackground();
        if (drawable != null)
            drawable.draw(canvas);
        markerView.draw(canvas);

        return returnedBitmap;

    }

    private static Bitmap drawableToBitmap (Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }

        int width = drawable.getIntrinsicWidth();
        width = width > 0 ? width : 1;
        int height = drawable.getIntrinsicHeight();
        height = height > 0 ? height : 1;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE, PorterDuff.Mode.SRC_IN);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    private static String scanMarkerCustomType(String string) {
        String custom_type = "";
        if (string != null) {
            for (int i = 0; i < string.length(); i++) {
                int type = Character.getType(string.charAt(i));
                if (type == Character.SURROGATE || type == Character.OTHER_SYMBOL) {
                    custom_type += string.charAt(i);
                } else {
                    break;
                }
            }
        }
        if (custom_type.length() <= 0) {
            custom_type = "\uD83D\uDCAC";
        }
        return custom_type;
    }

    private void makeNotification(String content) {
        if (content == null && currentNotificationText == null ||
                (content != null &&  content.equals(currentNotificationText))) {
            return;
        }
        currentNotificationText = content;

        NotificationManager mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        if (content == null) {
            mNotificationManager.cancel(1001);
            return;
        }
        Intent notificationIntent = new Intent(this, MapsActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent
                .getActivity(this, 0, notificationIntent, FLAG_CANCEL_CURRENT);

        // Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                //.setSmallIcon(R.drawable.ic_notifications_black_24dp)
                .setSmallIcon(R.drawable.ic_chat)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(content)
                .setContentIntent(intent)
                .setPriority(PRIORITY_HIGH) //private static final PRIORITY_HIGH = 5;
                .setAutoCancel(true)
                //.setSound(soundUri)
                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS);
        mNotificationManager.notify(1001, mBuilder.build());
    }

    private synchronized void playSound(String sound) {

        int resource;
        switch (sound) {
            case "delete_point":
                resource = R.raw.delete_point;
                break;
            case "receive_message_in":
                resource = R.raw.receive_message_in;
                break;
            case "receive_message_out":
                resource = R.raw.receive_message_out;
                break;
            case "receive_point":
                resource = R.raw.receive_point;
                break;
            case "send_point":
                resource = R.raw.send_point;
                break;
            default:
                resource = R.raw.send;
        }
        if (mediaPlayer != null) {
            return;
            /*
            try {
                mediaPlayer.stop();
            }
            catch (Exception e) { }
            mediaPlayer.release();
            mediaPlayer = null;
            */
        }
        mediaPlayer = MediaPlayer.create(this/*getApplicationContext()*/, resource);
        if (mediaPlayer != null) {
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    if (mp != null) {
                        mp.release();
                        mediaPlayer = null;
                    }

                }
            });
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer _mediaPlayer) {
                    _mediaPlayer.start();
                }
            });
            //mediaPlayer.start();
        }

    }

    private static int tryParseInt(Object obj, int def) {
        int retVal;
        if (obj == null)
            return def;
        try {
            retVal = Integer.parseInt(""+obj);
        } catch (Exception e) {
            retVal = def;
        }
        return retVal;
    }
    private static long tryParseLong(Object obj, long def) {
        long retVal;
        if (obj == null)
            return def;
        try {
            retVal = Long.parseLong(""+obj);
        } catch (Exception e) {
            retVal = def;
        }
        return retVal;
    }

    /* PREFS ***********************************************************************************/

    private static String uniqueID = null;
    private static String sessionID;
    private static String displayName;
    private static String avatarName;
    private static Boolean onNotificationPoint;
    private static Boolean onNotificationRoom;
    private static HashMap<String,String> receivedPoints;
    private static HashMap<String,String> roomTimestamp;
    private static final String PREF_UNIQUE_ID = "PREF_UNIQUE_ID";
    private static final String PREF_SESSION_ID = "PREF_SESSION_ID";
    private static final String PREF_DISPLAY_NAME = "PREF_DISPLAY_NAME";
    private static final String PREF_AVATAR_NAME = "PREF_AVATAR_NAME";
    private static final String PREF_ON_NOTIFICATION_POINT = "PREF_ON_NOTIFICATION_POINT";
    private static final String PREF_ON_NOTIFICATION_ROOM = "PREF_ON_NOTIFICATION_ROOM";
    private static final String PREF_PATH_POINT = "PREF_PATH_POINT";
    private static final String PREF_RECEIVED_POINTS = "PREF_RECEIVED_POINTS";
    private static final String PREF_ROOM_TIMESTAMP = "PREF_ROOM_TIMESTAMP";

    private synchronized static String uniqueDeviceId(Context context) {
        if (uniqueID == null) {
            SharedPreferences sharedPrefs = context.getSharedPreferences(
                    PREF_UNIQUE_ID, Context.MODE_PRIVATE);
            uniqueID = sharedPrefs.getString(PREF_UNIQUE_ID, null);
            if (uniqueID == null) {
                uniqueID = UUID.randomUUID().toString();
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putString(PREF_UNIQUE_ID, uniqueID);
                editor.commit();
            }
        }
        return uniqueID;
    }
    private synchronized static void syncSettings(Context context) {
        syncSessionID(context);
        syncDisplayName(context);
        syncAvatarName(context);
        syncOnNotificationPoint(context);
        syncOnNotificationRoom(context);
        syncPathPoint(context);
        syncReceivedPoints(context);
        syncRoomTimestamp(context);
    }
    private synchronized static void syncSessionID(Context context) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(
                PREF_SESSION_ID, Context.MODE_PRIVATE);
        if (sessionID == null) {
            sessionID = sharedPrefs.getString(PREF_SESSION_ID, null);
        } else {
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putString(PREF_SESSION_ID, sessionID);
            editor.apply();
        }
    }
    private synchronized static void syncDisplayName(Context context) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(
                PREF_DISPLAY_NAME, Context.MODE_PRIVATE);
        if (displayName == null) {
            displayName = sharedPrefs.getString(PREF_DISPLAY_NAME, null);
        } else {
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putString(PREF_DISPLAY_NAME, displayName);
            editor.apply();
        }

    }
    private synchronized static void syncAvatarName(Context context) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(
                PREF_AVATAR_NAME, Context.MODE_PRIVATE);
        if (avatarName == null) {
            avatarName = sharedPrefs.getString(PREF_AVATAR_NAME, null);
        } else {
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putString(PREF_AVATAR_NAME, avatarName);
            editor.apply();
        }

    }
    private synchronized static void syncOnNotificationPoint(Context context) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(
                PREF_ON_NOTIFICATION_POINT, Context.MODE_PRIVATE);
        if (onNotificationPoint == null) {
            onNotificationPoint = sharedPrefs.getBoolean(PREF_ON_NOTIFICATION_POINT, false);
        } else {
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putBoolean(PREF_ON_NOTIFICATION_POINT, onNotificationPoint);
            editor.apply();
        }
    }
    private synchronized static void syncOnNotificationRoom(Context context) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(
                PREF_ON_NOTIFICATION_ROOM, Context.MODE_PRIVATE);
        if (onNotificationRoom == null) {
            onNotificationRoom = sharedPrefs.getBoolean(PREF_ON_NOTIFICATION_ROOM, false);
        } else {
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putBoolean(PREF_ON_NOTIFICATION_ROOM, onNotificationRoom);
            editor.apply();
        }
    }
    private synchronized static void syncPathPoint(Context context) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(
                PREF_PATH_POINT, Context.MODE_PRIVATE);
        if (syncPathPoint == null) {
            syncPathPoint = sharedPrefs.getString(PREF_PATH_POINT, null);
            if (syncPathPoint == null) {
                syncPathPoint = "";
            }
        } else {
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putString(PREF_PATH_POINT, syncPathPoint);
            editor.apply();
        }
    }
    private synchronized static void syncReceivedPoints(Context context) {
        if (receivedPoints == null) {
            try {
                receivedPoints = FileUtils.getSavedObjectFromPreference(context, PREF_RECEIVED_POINTS, "receivedPoints", HashMap.class);
                if (receivedPoints == null) {
                    receivedPoints = new HashMap<>();
                }
            } catch(Exception ex) {
                receivedPoints = new HashMap<>();
            }
        } else {
            try {
                FileUtils.saveObjectToSharedPreference(context, PREF_RECEIVED_POINTS, "receivedPoints", receivedPoints);
            } catch(Exception ex) {
            }
        }

    }
    private synchronized static void syncRoomTimestamp(Context context) {
        if (roomTimestamp == null) {
            try {
                roomTimestamp = FileUtils.getSavedObjectFromPreference(context, PREF_ROOM_TIMESTAMP, "roomTimestamp", HashMap.class);
                if (roomTimestamp == null) {
                    roomTimestamp = new HashMap<>();
                }
            } catch(Exception ex) {
                roomTimestamp = new HashMap<>();
            }

        } else {
            try {
                FileUtils.saveObjectToSharedPreference(context, PREF_ROOM_TIMESTAMP, "roomTimestamp", roomTimestamp);
            } catch(Exception ex) {
            }
        }
    }

    private String UID() {
        return uniqueDeviceId(MapsActivity.this);
    }

    /* LOCATION *********************************************************************************/

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private boolean mPermissionLocationDenied = false;

    /* FIREBASE ***********************************************************************************/

    private FirebaseAuth mAuth;

    private final static String MAIN_POINT = "clicks";
    private final static String MAIN_CHAT = "chat";
    private static String syncPathPoint;
    private static String newSyncPathPoint;

    private static String mainPoint() { return MAIN_POINT + "/" + syncPathPoint; }
    private static String mainPoint(String pathPoint) { return MAIN_POINT + "/" + pathPoint; }
    private static String mainChat() { return MAIN_CHAT + "/" + syncPathPoint; }
    private static String mainChat(String pathPoint) { return MAIN_CHAT + "/" + pathPoint; }

    private String MessageRef() { return mainChat() + "/room-messages"; }
    private String RoomRef() { return mainChat() + "/room-metadata"; }
    private String RoomRef(String pathPoint) { return mainChat(pathPoint) + "/room-metadata"; }
    private String UsersOnlineRef() { return mainChat() + "/user-names-online"; }
    private final static String ConnectedRef = ".info/connected";
    private String UserRef(String userId) { return mainChat() + "/users/" + userId; }
    private String UserRoomRef(String userId, String roomId) { return UserRef(userId) + "/rooms/" + roomId; }
    private String SessionRef(String userId) { return UserRef(userId) + "/sessions"; }
    private String UserPresenceRef(String roomId, String userId, String sessionId) { return mainChat() + "/room-users/" + roomId + "/" + userId + "/" + sessionId; }

    /* MEMBERS ***********************************************************************************/

    private enum AppState {
        ON_LEAVE_MARKER,
        ON_APPEND_MARKER,
        ON_SELECT_MARKER,
        ON_SELECT_FIND_ADDRESS_MARKER,
        ON_CHAT_SHOW,
        ON_MOVE_APPEND_MARKER,
        ON_DEFAULT
    }
    private AppState appState = AppState.ON_DEFAULT;

    private BroadcastReceiver receiver;

    private GoogleMap mMap;

    private ImageButton mSettingsButton;
    private EditText mFindAddressEdit;
    private ImageButton mFindAddressButton;

    private LinearLayout pulseLayout;
    private View pulseView;
    private AnimationDrawable flashAnimation;
    private ObjectAnimator pulseAnimation;

    private ImageButton mPlusButton;
    private ImageButton mMinusButton;
    private ImageButton mLocationButton;

    private LinearLayout mTitleView;
    private LinearLayout mEditView;
    private RelativeLayout mChatLayout;
    private LinearLayout mChatView;

    //private Button mMarkerExpireButton;
    //private Button mMarkerTTLButton;
    private ImageButton mMarkerTransferButton;
    private ImageButton mChannelSettings;
    private TextView mMarkerView;
    private EditText mMarkerEdit;
    private ImageButton mMarkerSendButton;

    private ImageButton mChatSettings;
    private TextView mMarkerChatView;

    private MediaPlayer mediaPlayer;

    //private Timer mTimer;
    //private ExpireTimerTask mExpireTimerTask;
    private AlertDialog dialogTransferPoint;
    private AlertDialog authDialog;

    private TileOverlay mTileOverlay;

    /* MESSAGES *********************************************************************************/

    private ImageLoader imageLoader;
    private MessagesListAdapter<Message> messagesAdapter;
    private MessageInput messagesInput;
    private MessagesList messagesList;

    /* PARAMS ***********************************************************************************/

    private boolean onForeground;
    private double currentLatitude;
    private double currentLongitude;
    private float currentZoom;

    private int currentMapType = 0;
    private String currentPointInRoom;
    private String currentRoomOnPoint;
    //private int currentTTL = 1;
    private Marker currentMarker;
    private LatLng currentCoordinate;
    //private Calendar currentDateTTL;
    private boolean followMap;
    private boolean needRefresh;

    public static final String REFRESH_ACTION_FILTER = "su.idev.chatmap.action.REFRESH";

    private String currentNotificationText;

    private HashMap<String,Object> point_marker = new HashMap<>(); // pointId -> marker,point
    private HashMap<String,ChildEventListener> room_event = new HashMap<>(); // roomId -> events
    private HashMap<String,Integer> room_unreaded = new HashMap<>(); // roomId -> unreaded
    private ChildEventListener pointListener;

    private HashMap<String,String> room_del_point = new HashMap<>(); // roomId -> pointId
    private HashMap<String,Object> presence_bits = new HashMap<>(); // manual

    private DatabaseReference connectedRef;

    private String currentPointOnMarker()
    {
        //
        return currentMarker != null ? currentMarker.getSnippet() : null;
    }

    /* CACHE ***********************************************************************************/


}
