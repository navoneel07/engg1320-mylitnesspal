package com.checc.mylitnesspal;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.github.clans.fab.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private FloatingActionButton addMeal;

    private FirebaseDatabase db;
    private DatabaseReference ref;

    private RecyclerView recyclerView;

    private long nextMealNo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseDatabase.getInstance();
        ref = db.getReference().child("Meals");
        ref.keepSynced(true);

        addMeal = findViewById(R.id.add_meal_btn);

        addMeal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //open activity for adding meal
                Intent addMealActivity = new Intent(getApplicationContext(), AddMealActivity.class);
                startActivity(addMealActivity);
            }
        });
        db.getReference().child("Meal Number").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                nextMealNo = (long) dataSnapshot.getValue();
                db.getReference().child("Meals").child("Meal " + nextMealNo).removeValue();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        recyclerView = findViewById(R.id.recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));


    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseRecyclerAdapter<Meal, MealViewHolder> firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Meal, MealViewHolder>
                (Meal.class, R.layout.meal_card, MealViewHolder.class, ref) {
            @Override
            protected void populateViewHolder(MealViewHolder viewHolder, Meal model, int position) {
                viewHolder.SetName(model.getName());
                viewHolder.SetKcals(model.getkCal());
            }
        };
        recyclerView.setAdapter(firebaseRecyclerAdapter);    }

    public static class MealViewHolder extends RecyclerView.ViewHolder
    {
        View mView;
        public MealViewHolder(View itemView){
            super(itemView);
            mView = itemView;
        }

        public void SetName(String name){
            TextView titleView = mView.findViewById(R.id.meal_card_title);
            titleView.setText(name);
        }

        public void SetKcals(String kcal){
            TextView kcalView = mView.findViewById(R.id.meal_card_cals);
            kcalView.setText(kcal + " kcal");
        }
    }
}
