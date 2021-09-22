package com.postpc.Sheed.makeMatches;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.material.imageview.ShapeableImageView;
import com.postpc.Sheed.R;
import com.postpc.Sheed.SheedApp;
import com.postpc.Sheed.SheedUser;
import com.postpc.Sheed.MatchMakerEngine;
import com.postpc.Sheed.Utils;
import com.postpc.Sheed.database.SheedUsersDB;
import com.squareup.picasso.Picasso;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.postpc.Sheed.Utils.SECOND;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MakeMatchesFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MakeMatchesFragment extends Fragment {

    private final static String TAG = "Sheed MakeMatches Frag";
    private final static String PAGE_TITLE = "Sheed";


    private final static float ALPHA_FADE_OUT = 0f;
    private final static float ALPHA_FADE_IN = 1f;
    private final static float GO_RIGHT = 360f;
    private final static float GO_LEFT = -GO_RIGHT;

    private final static int RHS = 0;
    private final static int LHS = 1;


    private SheedUser sheedUser;
    SheedUsersDB db;

    TextView rhsNameView;
    TextView lhsNameView;
    TextView page_title;
    ShapeableImageView rhsImage;
    ShapeableImageView lhsImage;
    ImageButton acceptMatchFab;
    ImageButton declineMatchFab;
    TextView headerView;
    View swipeDetector;
    ImageButton backButton;
    ConstraintLayout rhsBlock;
    ConstraintLayout lhsBlock;

    //TextView matchesNotFoundView;

    boolean noMatchesAreFound = true;

    Pair<SheedUser, SheedUser> suggestedMatch;

    Pair<Float, Float> originalPosition;

    public MakeMatchesFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment MakeMatchesFragment.
     */
    public static MakeMatchesFragment newInstance() {
        MakeMatchesFragment fragment = new MakeMatchesFragment();
        Bundle args = new Bundle();

        // in case you want to pass arguments:
        //args.putSerializable(USER_INTENT_SERIALIZABLE_KEY, sheedUser);
        fragment.setArguments(args);
        return fragment;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final boolean[] waitingForFS = {true};

        db.getCurrentUserLiveData().observe(getViewLifecycleOwner(), sheedUser -> {
            if (noMatchesAreFound) {
                matchLoopExecutorHelper();
            }
        });

        matchLoopExecutorHelper();

//        final LiveData<HashMap<String, SheedUser>> communityLiveData = db.getCommunityLiveData();
//        communityLiveData.observe(getViewLifecycleOwner(), stringSheedUserHashMap -> {
//            if (waitingForFS[0]){
//                matchLoopExecutorHelper();
//                waitingForFS[0] = false;
//            }
//
//        });

        acceptMatchFab.setOnClickListener(v -> onAcceptMatchAction());
        declineMatchFab.setOnClickListener(v -> onDeclineMatchAction());

        acceptMatchFab.bringToFront();
        declineMatchFab.bringToFront();

        swipeDetector.setOnTouchListener(new OnSwipeTouchListener(getContext()) {
            @Override
            public void onSwipeRight() {
                if (!noMatchesAreFound)
                {
                    onAcceptMatchAction();
                }
            }

            @Override
            public void onSwipeLeft() {
                if (!noMatchesAreFound)
                {
                    onDeclineMatchAction();
                }
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (sheedUser == null) {
            if (db == null) {
                db = SheedApp.getDB();
            }
            sheedUser = db.currentSheedUser;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_make_matches, container, false);
        headerView = view.findViewById(R.id.header_title);
        headerView.setText("user retrieved from database: " + sheedUser.firstName + " " + sheedUser.lastName);

        rhsNameView = view.findViewById(R.id.rhs_name);
        lhsNameView = view.findViewById(R.id.lhs_name);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            setPageTitle();
        }
        backButton = getActivity().findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            getActivity().onBackPressed();
        });

        rhsImage = view.findViewById(R.id.rhs_img);
        lhsImage = view.findViewById(R.id.lhs_img);

        acceptMatchFab = view.findViewById(R.id.like_button);
        declineMatchFab = view.findViewById(R.id.dislike_button);

        swipeDetector = view.findViewById(R.id.swipe_detector);

        lhsBlock = view.findViewById(R.id.lhs_block);
        rhsBlock = view.findViewById(R.id.rhs_block);
        saveViewsOriginalPosition();
        return view;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setPageTitle() {
        page_title = getActivity().findViewById(R.id.page_title);
        page_title.setTypeface(getResources().getFont(R.font.milla_cilla));
        page_title.setText(PAGE_TITLE);
        page_title.setTextSize(40);
        page_title.setTextColor(Color.parseColor(Utils.PRIMARY_ORANGE));
    }

    private void fillRhsUser(SheedUser sheedUser) {

        setHeaderInMatch();
        rhsNameView.setText(sheedUser.firstName);
        Picasso.with(getContext()).load(sheedUser.imageUrl).centerCrop().fit().into(rhsImage);
        fadeUsersIn(RHS);
    }

    private void fillLhsUser(SheedUser sheedUser) {

        setHeaderInMatch();
        lhsNameView.setText(sheedUser.firstName);
        Picasso.with(getContext()).load(sheedUser.imageUrl).centerCrop().fit().into(lhsImage);
        fadeUsersIn(LHS);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void matchLoopExecutorHelper() {


        suggestedMatch = MatchMakerEngine.getMatchFromWorker();

//        final Pair<String, String> pair = MatchMakerEngine.makeMatch();
//
//        db.downloadUserAndDo(pair.first, this::fillLhsUser);
//        db.downloadUserAndDo(pair.second, this::fillRhsUser);

        noMatchesAreFound = suggestedMatch == null;

        if (noMatchesAreFound) {
            onMatchesNotFound();
        } else {
//            int delay = (isFirstIterLhs && isFirstIterRhs) ? 0 : 1;
//            Handler handler = new Handler();
//            handler.postDelayed(this::onMatchFound, delay * SECOND);
            onMatchFound();
        }
    }

    private void onMatchFound(){
        setButtonsVisibility(VISIBLE);
        fillLhsUser(suggestedMatch.first);
        fillRhsUser(suggestedMatch.second);
    }

    private void onAcceptMatchAction() {

        // Can add animations

        // Update DB to let users know they have been matched
        if (suggestedMatch != null) {
            db.setMatched(suggestedMatch.first, suggestedMatch.second, db.currentSheedUser);
        }
        headerView.setText("Love is in the air !"); // I added this just to see that the listeners indeed work
        roundEndRoutine();
    }

    private void onDeclineMatchAction() {

        // Can add animations

        // Update DB for matching algorithm improvement
        headerView.setText("Better luck next time");  // I added this just to see that the listeners indeed work
        roundEndRoutine();
    }

    private void matchLoopExecutor(Integer delaySecs) {

        Handler handler = new Handler();
        handler.postDelayed(this::matchLoopExecutorHelper, delaySecs * SECOND);
    }

    private void roundEndRoutine() {

        fadeUsersOut(LHS);
        fadeUsersOut(RHS);
        matchLoopExecutor(1);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void onMatchesNotFound() {

        headerView.setText(R.string.add_more_friends);
        setUsersViewVisibility(VISIBLE);

        lhsImage.setImageResource(R.mipmap.ic_launcher_foreground);
        rhsImage.setImageResource(R.mipmap.ic_launcher_foreground);
        lhsNameView.setText("");
        rhsNameView.setText("");

        fadeUsersIn(LHS);
        fadeUsersIn(RHS);

        setButtonsVisibility(INVISIBLE);
    }

    private void setUsersViewVisibility(int status) {

        if (status == GONE || status == VISIBLE || status == INVISIBLE) {
            lhsImage.setVisibility(status);
            lhsNameView.setVisibility(status);
            rhsImage.setVisibility(status);
            rhsNameView.setVisibility(status);
        }
    }

    private void setButtonsVisibility(int status) {

        if (status == GONE || status == VISIBLE || status == INVISIBLE) {

            boolean enabled = status == VISIBLE;

            acceptMatchFab.setVisibility(status);
            declineMatchFab.setVisibility(status);

            acceptMatchFab.setEnabled(enabled);
            declineMatchFab.setEnabled(enabled);
        }
    }

    private void setHeaderInMatch() {
        headerView.setText("We think it's a great match !");
    }

    private void saveViewsOriginalPosition() {

        originalPosition = new Pair<>(lhsBlock.getTranslationX(), rhsBlock.getTranslationX());
    }

    private void fadeUsersIn(int side) {

        if (side == LHS) {
            if (lhsBlock.getTranslationX() != originalPosition.first) {
                animate(lhsBlock, GO_RIGHT, ALPHA_FADE_IN);
            }
        }
        else {       // RHS
            if (rhsBlock.getTranslationX() != originalPosition.second) {
                    animate(rhsBlock, GO_LEFT, ALPHA_FADE_IN);
            }
        }

    }

    private void fadeUsersOut( int side){
        if (side == LHS) {
            animate(lhsBlock, GO_LEFT, ALPHA_FADE_OUT);
        } else {       // RHS
            animate(rhsBlock, GO_RIGHT, ALPHA_FADE_OUT);
        }
    }

    private void animate(View view, float translationX, float alpha){
        view.animate().translationXBy(translationX).alpha(alpha).setDuration(SECOND / 2).
                start();
    }
}