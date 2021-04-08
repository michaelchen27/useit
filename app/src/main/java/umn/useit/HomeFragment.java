package umn.useit;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HomeFragment extends Fragment implements HomeCardAdapter.ItemClickListener{

    private TextView welcome;
    private TextView level;
    private TextView asked;
    FragmentManager fm = getFragmentManager();

    HomeCardAdapter adapter;
    ArrayList<String> problemTitles = new ArrayList<>();

    //Firebase
    private final FirebaseDatabase database = FirebaseDatabase.getInstance();
    private final DatabaseReference databaseUsers = database.getReference("Users");
    private final DatabaseReference databaseProblems = database.getReference("Problems");

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstance) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //GUI Init
        welcome = (TextView) Objects.requireNonNull(getView()).findViewById(R.id.welcome);
        level = (TextView) Objects.requireNonNull(getView()).findViewById(R.id.level);
        asked = (TextView) Objects.requireNonNull(getView()).findViewById(R.id.asked);

        // Init =============================================================================
        FirebaseUser curr_user = FirebaseAuth.getInstance().getCurrentUser();

        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .edit()
                .putInt("postTotal", 999999999).apply(); //lol



        String curr_userUid = curr_user.getUid();
        DatabaseReference userRow = databaseUsers.child(curr_userUid);

        //Get user
        userRow.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                User user = mutableData.getValue(User.class);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError databaseError, boolean b, @Nullable DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if(user != null) welcome.setText(String.format("Hi, %s %s!", user.getFirstname(), user.getLastname()));
            }
        }); //runTransaction()


        //Get User's Post
        Query query = databaseProblems.orderByChild("date");
        getDB(query);
    } //onViewCreated()

    public void showCards(List<Problem> problems) {
        RecyclerView recyclerView = getView().findViewById(R.id.rvHomeCard);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setReverseLayout(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        adapter = new HomeCardAdapter(getActivity(), problems);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);
    } //showCards()


    public void sendTotal(int total) {
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .edit()
                .putInt("postTotal", total).apply(); //buggy when firebase db is cleared.
    } //sendTotal()


    public void getDB(Query query) {
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Problem> problems = new ArrayList<>();
                if (snapshot.exists()) {
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        Problem problem = dataSnapshot.getValue(Problem.class);
                        problems.add(problem);
                    }
                    showCards(problems);
                    sendTotal(problems.size());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        }); //Get User's Post

    } //getDB()

    @Override
    public void onItemClick(View view, int position) {
        String title = adapter.getItem(position).getTitleProblem();
        String desc = adapter.getItem(position).getDescProblem();
        int id = adapter.getItem(position).getId();
        int seen = adapter.getItem(position).getSeen();
        seen++;

        databaseProblems.child(String.valueOf(id)).child("seen").setValue(seen);
//        Toast.makeText(getActivity(), adapter.getItem(position).getTitleProblem() + " viewed", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getActivity(), DetailActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("desc", desc);
        startActivity(intent);
    }

}
