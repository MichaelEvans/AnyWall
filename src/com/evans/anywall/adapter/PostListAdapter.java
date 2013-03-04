package com.evans.anywall.adapter;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.TwoLineListItem;

import com.evans.anywall.model.Post;

public class PostListAdapter extends ArrayAdapter<Post>{

	private Context context;
	private List<Post> posts;

	public PostListAdapter(Context context, List<Post> posts) {
		super(context, android.R.layout.two_line_list_item, posts);
		this.context = context;
		this.posts = posts;
	}

	@Override
	public int getCount() {
		return posts.size();
	}

	@Override
	public Post getItem(int position) {
		return posts.get(position);
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@SuppressWarnings("deprecation")
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TwoLineListItem twoLineListItem;
		Post p = getItem(position);
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			twoLineListItem = (TwoLineListItem) inflater.inflate(
					android.R.layout.simple_list_item_2, null);
		} else {
			twoLineListItem = (TwoLineListItem) convertView;
		}
		TextView text1 = twoLineListItem.getText1();
		TextView text2 = twoLineListItem.getText2();
		
		text1.setText(p.getBody());
		text2.setText(p.getUser().getUsername());
		return twoLineListItem;
	}

}
