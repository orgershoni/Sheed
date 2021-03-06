package com.postpc.Sheed;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LiveData;
import androidx.work.WorkInfo;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.Toast;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ListenerRegistration;
import com.postpc.Sheed.Connections.ConnectionsFragment;
import com.postpc.Sheed.database.SheedUsersDB;
import com.postpc.Sheed.makeMatches.MakeMatchesFragment;
import com.postpc.Sheed.profile.ProfileFragment;


import java.util.Date;
import java.util.List;

import static com.postpc.Sheed.Utils.WORKER_JOB_END_TIME;
import static com.postpc.Sheed.Utils.WORK_MANAGER_TAG;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "SheedApp Main Activity";
    SheedUsersDB db;
    Context context;
    BottomNavigationView bottomNavigationView;
    SheedUser sheedUser;
    ImageButton backButton;
    ListenerRegistration currentUserCommunityListener;
    String passwordInput;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try{
            Thread.sleep(1500);
        }
        catch(InterruptedException ex){
            Thread.currentThread().interrupt();
        }
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Intent intentOpenedMe = getIntent();
        passwordInput = intentOpenedMe.getStringExtra("password");


        backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v->{onBackPressed(); });

        context = this;
        if (db == null)
        {
            db = SheedApp.getDB();
        }

        //TODO: remove the following debugging line:
        //db.saveUserIdToSP("fake@mail");
//        db.removeUserIdFromSP();
        setJobsObserver();
        String userId = db.getIdFromSP();
        if (userId == null)
        {
            Log.i(TAG, "user id from sp is null");
            Intent launchActivity = new Intent(context, ActivityStart.class);
            startActivity(launchActivity);
        }
        else
        {
            Log.i(TAG, "user id from sp: " + userId);
            bottomNavigationView = findViewById(R.id.bottomNavigationView);
            bottomNavigationView.setOnNavigationItemSelectedListener(navigationItemSelectedListener);
            bottomNavigationView.getMenu().getItem(1).setChecked(true);

            db.downloadUserAndDo(userId, sheedUser -> {
            // get the user from db and move to make matches screen
                if (sheedUser == null)
                {
                    Toast.makeText(this, "The user " + userId + " does not exists in App Data Base", Toast.LENGTH_LONG).show();
                    db.logOut();
                    //db.removeUserIdFromSP();
                }
                else if (!sheedUser.password.equals(passwordInput) && passwordInput != null )
                {
                    Toast.makeText(this, "incorrect password", Toast.LENGTH_LONG).show();
                    db.logOut();
                    //db.removeUserIdFromSP();
                    //startActivity(new Intent(context, MainActivity.class));
                }
                else
                {
                    db.logIn(sheedUser);
                    //db.currentSheedUser = sheedUser;
                    handleListeners();
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N){
                        db.loadCurrentFriends(sheedUsers -> {
                            // do noting - only make sure that list is up to date
                        });
                    }
                    getSupportFragmentManager().beginTransaction().replace(R.id.container, makeMatchesFragment).commit();
                }

            });
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void handleListeners() {
        if (currentUserCommunityListener != null) {
            currentUserCommunityListener.remove();
        }
        currentUserCommunityListener = db.listenToCommunityChanges();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (currentUserCommunityListener != null)
        {
            currentUserCommunityListener.remove();
        }
    }

    ProfileFragment profileFragment = ProfileFragment.newInstance();
    ConnectionsFragment connectionsFragment = ConnectionsFragment.newInstance();
    MakeMatchesFragment makeMatchesFragment = MakeMatchesFragment.newInstance();

    private BottomNavigationView.OnNavigationItemSelectedListener navigationItemSelectedListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.profile:
                            getSupportFragmentManager().beginTransaction().replace(R.id.container, profileFragment).addToBackStack(String.valueOf(R.id.profile)).commit();
                            return true;

                        case R.id.make_matches_navigate:
                            getSupportFragmentManager().beginTransaction().replace(R.id.container, makeMatchesFragment).addToBackStack(String.valueOf(R.id.make_matches_navigate)).commit();
                            return true;

                        case R.id.your_matches_navigate:
                            getSupportFragmentManager().beginTransaction().replace(R.id.container, connectionsFragment).addToBackStack(String.valueOf(R.id.your_matches_navigate)).commit();
                            return true;
                    }
                    return false;
                }
            };

    @Override
    public void onBackPressed() {
        Log.i(TAG, "back stack size: " + getSupportFragmentManager().getBackStackEntryCount());

        int stackSize = getSupportFragmentManager().getBackStackEntryCount();
        if (stackSize > 0) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            if(stackSize > 1){
                int idx = stackSize - 2;
                String prevPage = fragmentManager.getBackStackEntryAt(idx).getName();
                Log.i(TAG, "last page was " + prevPage);
                updateNavigationBarState(Integer.parseInt(prevPage));
        }
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    private void updateNavigationBarState(int actionId){
        Menu menu = bottomNavigationView.getMenu();
        menu.findItem(actionId).setChecked(true);
    }


    public void setJobsObserver(){
        LiveData<List<WorkInfo>> matchingAlgoRun = db.workManager.getWorkInfosByTagLiveData(WORK_MANAGER_TAG);
        matchingAlgoRun.observe(this, workInfos -> {
        if (db.currentSheedUser == null) {return;}
            for (WorkInfo workInfo : workInfos)
            {
                if (workInfo.getState() == WorkInfo.State.SUCCEEDED){
                    final long endTime = workInfo.getOutputData().getLong(WORKER_JOB_END_TIME, 0L);
                    final long lastEndTime = db.currentSheedUser.getLastRun();
                    Timestamp endTimeStamp = new Timestamp(new Date(endTime));

                    if (endTime > lastEndTime) {
                        Log.d("WORKER_TIME", "last algo run was " + db.currentSheedUser.getLastStatus().toString() +". updated to " + endTimeStamp);
                        db.currentSheedUser.saveStatus(endTimeStamp);
                        db.updateUser(db.currentSheedUser);

                    }
                }
            }

        });
    }
}

