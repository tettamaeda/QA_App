package jp.techacademy.tetta.maeda.qa_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class QuestionDetailActivity extends AppCompatActivity {

    private ListView mListView;
    private Question mQuestion;
    private QuestionDetailListAdapter mAdapter;
    private boolean fav = false;
    private ArrayList<String> mFavList = null;
    private String mFavs;
    private String mQNum = "0";

    private DatabaseReference mAnswerRef;

    FirebaseAuth mAuth;
    DatabaseReference mDataBaseReference;

    private ChildEventListener mEventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            HashMap map = (HashMap) dataSnapshot.getValue();

            String answerUid = dataSnapshot.getKey();

            for(Answer answer : mQuestion.getAnswers()) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid.equals(answer.getAnswerUid())) {
                    return;
                }
            }

            String body = (String) map.get("body");
            String name = (String) map.get("name");
            String uid = (String) map.get("uid");

            Answer answer = new Answer(body, name, uid, answerUid);
            mQuestion.getAnswers().add(answer);
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_detail);

            // 渡ってきたQuestionのオブジェクトを保持する
            Bundle extras = getIntent().getExtras();
            mQuestion = (Question) extras.get("question");
            mQNum = (String) mQuestion.getQnum();

            mDataBaseReference = FirebaseDatabase.getInstance().getReference();
            mAuth = FirebaseAuth.getInstance();
            FirebaseUser user = mAuth.getCurrentUser();

            if (user == null) {
                Button FavButton = (Button) findViewById(R.id.favoriteButton);
                FavButton.setVisibility(View.INVISIBLE);
            } else {

                Button FavButton = (Button) findViewById(R.id.favoriteButton);
                FavButton.setVisibility(View.VISIBLE);

                mFavList = new ArrayList<String>();
                mFavs = "";

                DatabaseReference userRef = mDataBaseReference.child(Const.UsersPATH).child(user.getUid());
                userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        Map data = (Map) snapshot.getValue();
                        mFavs = (String) data.get("favList");
                        if (mFavs != null) {
                            if (mFavs.indexOf(",") != -1) {
                                String[] temp = mFavs.split(",");
                                for (int i = 0; i < temp.length; i++) {
                                    mFavList.add(temp[i]);
                                }
                            } else {
                                mFavList.add(mFavs);
                            }

                        }

                        fav = favCheck(mFavList, mQNum);
                        Button FavButton = (Button) findViewById(R.id.favoriteButton);
                        if (fav == true) {
                            FavButton.setBackgroundColor(Color.YELLOW);
                        } else {
                            FavButton.setBackgroundColor(Color.GRAY);
                        }
                        FavButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (fav == true) {
                                    fav = false;
                                    v.setBackgroundColor(Color.GRAY);
                                    // favListから削除
                                    mFavList.remove(mFavList.indexOf(mQNum));
                                } else {
                                    fav = true;
                                    v.setBackgroundColor(Color.YELLOW);
                                    // favListに追加
                                    if (mFavList == null) mFavList = new ArrayList<String>();
                                    mFavList.add(mQNum);

                                }
                                // favList更新
                                mDataBaseReference = FirebaseDatabase.getInstance().getReference();
                                mAuth = FirebaseAuth.getInstance();
                                FirebaseUser user = mAuth.getCurrentUser();
                                DatabaseReference userRef = mDataBaseReference.child(Const.UsersPATH).child(user.getUid());
                                Map<String, String> data = new HashMap<String, String>();
                                String temp = "";
                                for (int i = 0; i < mFavList.size(); i++) {
                                    temp = temp + mFavList.get(i);
                                    if (i != mFavList.size() - 1) temp = temp + ",";
                                }

                                data.put("favList", temp);
                                userRef.setValue(data);
                            }
                        });

                    }

                    @Override
                    public void onCancelled(DatabaseError firebaseError) {
                    }

                });

            }

            setTitle(mQuestion.getTitle());

            // ListViewの準備
            mListView = (ListView) findViewById(R.id.listView);
            mAdapter = new QuestionDetailListAdapter(this, mQuestion);
            mListView.setAdapter(mAdapter);
            mAdapter.notifyDataSetChanged();

            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // ログイン済みのユーザーを取得する
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                    if (user == null) {
                        // ログインしていなければログイン画面に遷移させる
                        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                        startActivity(intent);
                    } else {
                        // Questionを渡して回答作成画面を起動する
                        // --- ここから ---
                        Intent intent = new Intent(getApplicationContext(), AnswerSendActivity.class);
                        intent.putExtra("question", mQuestion);
                        startActivity(intent);
                        // --- ここまで ---
                    }
                }
            });

            DatabaseReference dataBaseReference = FirebaseDatabase.getInstance().getReference();
            mAnswerRef = dataBaseReference.child(Const.ContentsPATH).child(String.valueOf(mQuestion.getGenre())).child(mQuestion.getQuestionUid()).child(Const.AnswersPATH);
            mAnswerRef.addChildEventListener(mEventListener);
    }

    private boolean favCheck(ArrayList<String> favList, String QNum){
        for(int i = 0; i < favList.size(); i++){
            if(favList.get(i).equals(QNum)) return true;
        }
        return false;
    }
}
