package com.why94.view.recycler.layoutmanager;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.why94.recycler.RecyclerAdapter;

import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private BigHeadSquareGridLayoutManager mLayoutManager;
    private RecyclerAdapter mRecyclerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRecyclerView = findViewById(R.id.recycler_view);
        mLayoutManager = new BigHeadSquareGridLayoutManager();
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerAdapter = new RecyclerAdapter(this);
        mRecyclerView.setAdapter(mRecyclerAdapter);

        for (int i = 0; i < 100; i++) {
            mRecyclerAdapter.add(ItemHolder.class, i);
        }

    }

    private Random mRandom = new Random();

    class ItemHolder extends RecyclerAdapter.Holder<Integer> implements View.OnClickListener {
        private TextView mTextView;

        public ItemHolder(ViewGroup group) {
            super(group, new TextView(group.getContext()));
            mTextView = (TextView) itemView;
            mTextView.setOnClickListener(this);
            mTextView.setGravity(Gravity.CENTER);
            mTextView.setBackgroundColor(Color.argb(0x11, mRandom.nextInt(256), mRandom.nextInt(256), mRandom.nextInt(256)));
        }

        @Override
        protected void bindData(int position, Integer data, @NonNull List<Object> payloads) {
            if (data <= 12) {
                mTextView.setText("移除此项");
            } else if (data <= 20) {
                mTextView.setText("span count = " + (data - 12));
            } else if (data <= 28) {
                mTextView.setText("head weight = " + (data - 20));
            } else {
                mTextView.setText(String.valueOf(data));
            }
        }

        @Override
        public void onClick(View v) {
            if (data <= 12) {
                mRecyclerAdapter.remove(getAdapterPosition());
            } else if (data <= 20) {
                try {
                    mLayoutManager.setSpanCount(data - 12);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else if (data <= 28) {
                try {
                    mLayoutManager.setHeadWeight(data - 20);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                mTextView.setText(String.valueOf(data));
            }
        }
    }
}
