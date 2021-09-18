package com.postpc.Sheed;

import android.util.Log;
import android.util.Pair;

import com.postpc.Sheed.database.SheedUsersDB;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import static com.postpc.Sheed.Utils.USER1_EMAIL;
import static com.postpc.Sheed.Utils.USER1_TEST;
import static com.postpc.Sheed.Utils.USER2_EMAIL;
import static com.postpc.Sheed.Utils.USER2_TEST;
import static com.postpc.Sheed.makeMatches.FindMatchWorker.String2Pair;

public class MatchMakerEngine {

    static SheedUsersDB db = SheedApp.getDB();

    static Query<Gender> genderQuery = sheedUser -> sheedUser.gender;
    static Query<Gender> interestedInQuery = sheedUser -> sheedUser.interestedIn;


    // This is a test make match

    public static List<String> makeMatch()
    {
        return new ArrayList<>(Arrays.asList(USER1_EMAIL, USER2_EMAIL));
    }

    public static Pair<SheedUser, SheedUser> getMatchFromWorker()
    {
        Queue<String> formattedPairs = new LinkedList<>(db.currentSheedUser.pairsToSuggest);
        String pair = formattedPairs.poll();
        if (pair != null)
        {
            Pair<String, String> pairStr = String2Pair(pair);
            db.currentSheedUser.pairsToSuggest = new ArrayList<>(formattedPairs);
            db.updateUser(db.currentSheedUser);

            if (pairStr != null && db.userFriendsMap != null)
            {
                SheedUser user1 = db.userFriendsMap.get(pairStr.first);
                SheedUser user2 = db.userFriendsMap.get(pairStr.second);
                return new Pair<>(user1, user2);
            }
            else
            {
                Log.d("MatchEngine", "string2pair method failed");
            }


        }
        return null;
    }
}
