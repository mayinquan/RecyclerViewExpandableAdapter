package com.myq.android.adapter.example;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.my.android.recyclerexpandableadapter.R;
import com.myq.android.adapter.DividerDecoration;
import com.myq.android.adapter.ExpandableAdapter;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;

public class MainActivity extends Activity {
	private RecyclerView mRecyclerView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mRecyclerView = new RecyclerView(this);
		setContentView(mRecyclerView);

		DividerDecoration dividerDecoration = new DividerDecoration.Builder(this)
				.setColorResource(android.R.color.white).setHeight(1f).build();
		mRecyclerView.addItemDecoration(dividerDecoration);

		LinearLayoutManager llm = new LinearLayoutManager(this);
		ExpandableAdapter<Group, Item> adapter = new ExpandableAdapter<Group, Item>(this, llm) {

			@Override
			public ViewHolder onCreateGroupViewHolder(ViewGroup parent) {
				LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
				TextView tv = new TextView(MainActivity.this);
				tv.setLayoutParams(lp);
				tv.setPadding(10, 20, 10, 20);
				tv.setTextColor(getResources().getColor(android.R.color.white));
				tv.setBackgroundResource(R.color.grey);
				return new ViewHolder(tv) {
				};
			}

			@Override
			public ViewHolder onCreateItemViewHolder(ViewGroup parent) {
				LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
				TextView tv = new TextView(MainActivity.this);
				tv.setLayoutParams(lp);
				tv.setPadding(10, 20, 10, 20);
				tv.setBackgroundResource(R.color.green);
				return new ViewHolder(tv) {
				};
			}

			@Override
			public void onBindGroupHolderData(ViewHolder holder,
					com.myq.android.adapter.ExpandableAdapter.BaseGroupData parent, Group group, boolean isExpanded) {
				((TextView) holder.itemView).setText(group.name);
			}

			@Override
			public void onBindItemHolderData(ViewHolder holder,
					com.myq.android.adapter.ExpandableAdapter.BaseChildData parent, Item child) {
				((TextView) holder.itemView).setText(child.des);
			}

			@Override
			public void onBindStickyHeaderViewHolderData(ViewHolder holder) {

			}
		};
		mRecyclerView.setAdapter(adapter);
		Map<Group, List<Item>> mDataMap = new LinkedHashMap<>();
		for (int i = 0; i < 20; i++) {
			Group group = new Group();
			group.name = "G " + i;
			List<Item> children = new ArrayList<Item>();
			for (int j = 0; j < 10; j++) {
				Item child = new Item();
				child.des = "Des " + j;
				children.add(child);
			}
			mDataMap.put(group, children);
		}
		adapter.setData(mDataMap);

	}

}

class Group {
	public String name;
}

class Item {
	public String des;
}