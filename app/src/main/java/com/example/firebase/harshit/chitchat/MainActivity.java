/**
 * Copyright Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.firebase.harshit.chitchat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ui.email.SignInActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.example.firebase.harshit.chitchat.BuildConfig;
import com.example.firebase.harshit.chitchat.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public static final String ANONYMOUS = "anonymous";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;
    private static final int RC_PHOTO_PICKER = 2;
    private static final String FRIENDLY_MSG_LENGTH_KEY = "friendly_msg_length";

    private ListView mMessageListView;
    private MessageAdapter mMessageAdapter;
    private ProgressBar mProgressBar;
    private ImageButton mPhotoPickerButton;
    private EditText mMessageEditText;
    private Button mSendButton;
    private static final int RC_SIGN_IN = 1;
    private String mUsername;
    private FirebaseDatabase mFirebaseDatabase;//this object is the entry point for our app to access the database
    private DatabaseReference mMessagesDatabaseReference;// a database reference object is a class that references a specific
    //part of the database,so this object will reference messages portion of database
private FirebaseRemoteConfig mfirebaseremoteconfig;
    private ChildEventListener mchildeventlistener;//a listener that is triggered whwner messages are added to firebase
    private FirebaseAuth mfirebaseauth;
    private FirebaseAuth.AuthStateListener mAuthstatelistener;
    private FirebaseStorage mfirebasestorage;
    private StorageReference mchatphotosstoragereference;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUsername = ANONYMOUS;
        // for any firebase service u need an instance to use it

        mFirebaseDatabase = FirebaseDatabase.getInstance();//to get default instance of the FirebaseDatabase class
        mfirebaseauth = FirebaseAuth.getInstance();
        mfirebasestorage=FirebaseStorage.getInstance();
         mfirebaseremoteconfig=FirebaseRemoteConfig.getInstance();
        //above is main access point to database
        //using this we can reference specific part of database
        mMessagesDatabaseReference = mFirebaseDatabase.getReference().child("messages");//getreference() gets access to root node
        //by specifiying child we metion the child of root we want to access to this particular node
        // Initialize references to views

        //here we specify the exact folder name we want to access from storage
        mchatphotosstoragereference=mfirebasestorage.getReference().child("chat_photos");

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageListView = (ListView) findViewById(R.id.messageListView);
        mPhotoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mSendButton = (Button) findViewById(R.id.sendButton);

        // Initialize message ListView and its adapter
        List<FriendlyMessage> friendlyMessages = new ArrayList<>();
        mMessageAdapter = new MessageAdapter(this, R.layout.item_message, friendlyMessages);
        mMessageListView.setAdapter(mMessageAdapter);

        // Initialize progress bar
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);

        // ImagePickerButton shows an image picker to upload a image for a message
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Fire an intent to show an image picker

                Intent ob=new Intent(Intent.ACTION_GET_CONTENT);
                ob.setType("image/jpeg");
                ob.putExtra(Intent.EXTRA_LOCAL_ONLY,true);
                //Extra used to indicate that an intent should only return data that is on the local device. This is a boolean extra; the default is false.
                // If true, an implementation should only allow the user to select data that is already on the device, not requiring it be downloaded from a remote service when opened.
                startActivityForResult(Intent.createChooser(ob,"complete action using"),RC_PHOTO_PICKER);
                //Builds a new ACTION_CHOOSER Intent that wraps
                // the given target intent(here ob), also optionally supplying a title.
                //we need integer constant to work as request code ,to uniquely identify the request(as here to open
                // system  photo picker)
            }
        });

        // Enable Send button when there's text to send
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});

        // Send button sends a message and clears the EditText
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Send messages on click
                FriendlyMessage fm = new FriendlyMessage(mMessageEditText.getText().toString(), mUsername, null);
                mMessagesDatabaseReference.push().setValue(fm);//adds new message data to databse ,push() creates unique push id for this
                //message data and adds it to firebase dataabase
                // Clear input box
                mMessageEditText.setText("");
            }
        });


        mAuthstatelistener = new FirebaseAuth.AuthStateListener() {
            @Override
            //firebaseAuth contains whether at that moment user is authenticated or not
            public void onAuthStateChanged(FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    //user is signed in
                    onSignedInInitialize(user.getDisplayName());
                    Toast.makeText(MainActivity.this, "You are signed in", Toast.LENGTH_SHORT);

                } else {
                    //user is signed out
                    onSignedOutCleanUp();
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)//using this phone saves users credential to log them in
                                    .setProviders(
                                            AuthUI.EMAIL_PROVIDER,
                                            AuthUI.GOOGLE_PROVIDER)


                                    .build(),
                            RC_SIGN_IN);//RC means request code
                }
            }
        };
        FirebaseRemoteConfigSettings configSettings=new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG).build();
        //if your apps build confid is debug type above is true,else if its release type it is false
        mfirebaseremoteconfig.setConfigSettings(configSettings);
        Map<String,Object> defaultconfigmap=new HashMap<>();
        defaultconfigmap.put(FRIENDLY_MSG_LENGTH_KEY,DEFAULT_MSG_LENGTH_LIMIT);
        mfirebaseremoteconfig.setDefaults(defaultconfigmap);
        fetchconfig();

    }

    private void fetchconfig() {
        long cacheexpiration=3600;
        if(mfirebaseremoteconfig.getInfo().getConfigSettings().isDeveloperModeEnabled()){
            cacheexpiration=0;
        }
        //mfirebaseremoteconfig will fetch values from firebase every 3600 sec,ie 1hr
        //the values fetched are cached in this remote config object
        mfirebaseremoteconfig.fetch(cacheexpiration).addOnSuccessListener(new OnSuccessListener<Void>() {
           // if fetching is successful,the new data will be cached into remoteconfigobject,else in onfailure
            //we will call applyretrievedlengthlimit,and it will use the already exisiting data in this object
            @Override
            public void onSuccess(Void unused) {
                mfirebaseremoteconfig.activateFetched();
                applyretrievedlengthlimit();
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG,"ERoor fetching config");//failure case occurs when u r offline
                        applyretrievedlengthlimit();

                    }
                });
    }
private void applyretrievedlengthlimit()//to apply changes that have been pushed from firebase remote config
 {
     Long friendly_msg_length=mfirebaseremoteconfig.getLong(FRIENDLY_MSG_LENGTH_KEY);
     //if fetching is successful u willl have new length sent from remote config.This happens when we call apply
     //retreivedlengthlimit from onsuccess above
     //in case of failure it will pick the value from cache.this happens on calling this mmethod from onfailure
     mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(friendly_msg_length.intValue())});
     Log.d(TAG,"msg length ="+friendly_msg_length.toString());
     //changing the new limit for messages we type on app;
     //earlier it was 1000 now it will change as per our sent value from remote config

}
    private void onSignedOutCleanUp() {
        //this method tears down the ui when user signs out
        mUsername = ANONYMOUS;
        mMessageAdapter.clear();//since user is not signed in he shouldnt be able to see msgs now
        detachReadListener();
    }

    private void detachReadListener() {
        if (mchildeventlistener != null)
            mMessagesDatabaseReference.removeEventListener(mchildeventlistener);
        mchildeventlistener = null;
    }

    private void onSignedInInitialize(String displayName) {
        mUsername = displayName;
        attachReadListener();

    }

    private void attachReadListener() {
        // only when user is signed in he should be able to see the messages
        //here we implement childeventlistener interface using anonymous class
        if (mchildeventlistener == null) //create childevent or readevent listener only if its value is null,if already exist no need to recreate
            mchildeventlistener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s)//this gets called whenever a new msg is inserted into messages list
                //when the listeener is first attached ,this method is triggered for every child msg that already exist in the list.

                //thereafter it will also be invoked for every new message added to meesages node
                {
                    //datasnapshot object contains data from firebase database at a specific location at the exact time the listener is called
                    FriendlyMessage ob = dataSnapshot.getValue(FriendlyMessage.class);//by passing friendlymsg class ,the code will deserialize the msg from the databse into
                    // plain friendlymsg object
                    mMessageAdapter.add(ob);//add this object to adapter now
                    //so here whenerv a new msg is added to database the onchildadded is called ,we retrieve the new msg in ob,and finally give it to
                    //adapter for displaying in list view


                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) //gets called when content of exisiting msg r changed
                {

                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) //gets called when exisiting msg is deleted
                {

                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s)//gets called when one of the message changes posoiton in the list
                {

                }

                @Override
                public void onCancelled(DatabaseError databaseError) //gets called when some sort of error occurs when  u r trying to make changes
                {

                }
            };

        mMessagesDatabaseReference.addChildEventListener(mchildeventlistener);//since this listneer is attached for messages node therefroe whwnever any child of this messages is changed the suitable method from above(overriden in implementing child listener) will be called
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (mAuthstatelistener != null)
            mfirebaseauth.removeAuthStateListener(mAuthstatelistener);//detach when app is no longer visible
        //when activity is invisible then also detach childevent listner and clear the message adapter
        detachReadListener();
        mMessageAdapter.clear();
    }

    @Override
    public void onActivityResult(int requestcode, int resultCode, Intent data) {
        super.onActivityResult(requestcode, resultCode, data);
        if (requestcode == RC_SIGN_IN)//  this is to check if we r returning from sign in  activity
        {
            if (resultCode == RESULT_OK)
                Toast.makeText(MainActivity.this, "SIGN IN SUCCESSFUL", Toast.LENGTH_SHORT);
            else {
                Toast.makeText(MainActivity.this, "SIGN IN CANCELLED", Toast.LENGTH_SHORT);
                finish();//finish the activity
            }
        }
        else if(requestcode==RC_PHOTO_PICKER&&resultCode==RESULT_OK)
        {//selected photo will be received as uri
            Uri selectedimageuri =data.getData();
            // suppose the uri is "content://local_images/foo/4",then the line below
            //will save the photo to chat_photos by creating child inside it with the name "4"(last path segment of uri)
            //photoref has reference to  location of image we r storage
            StorageReference photoref=mchatphotosstoragereference.child(selectedimageuri.getLastPathSegment());
            //upload file to storage
            //putfile will store our iamge using selectedimageuri to the above
            //location we created
            photoref.putFile(selectedimageuri).addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                  Uri downloadurl=  taskSnapshot.getDownloadUrl();//url of the file we just stored above
                    //storing the image file we uploaded to database
                    FriendlyMessage ob=new FriendlyMessage(null,mUsername,downloadurl.toString());
                    mMessagesDatabaseReference.push().setValue(ob);
                }
            });

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mAuthstatelistener!=null)
        mfirebaseauth.addAuthStateListener(mAuthstatelistener);//attach it as soon as app is visible





    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

             int id= item.getItemId();
             if(id==R.id.sign_out_menu)
             {
                   // to sign out
                 AuthUI.getInstance().signOut(this).addOnCompleteListener(new OnCompleteListener<Void>() {
                     @Override
                     public void onComplete(@NonNull Task<Void> task) {
                       startActivity(new Intent(MainActivity.this, SignInActivity.class));
                       finish();
                     }
                 })   ;

                 return true;
             }
             return super.onOptionsItemSelected(item);




    }
}