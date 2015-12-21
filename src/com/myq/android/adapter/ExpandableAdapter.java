package com.myq.android.adapter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.LayoutManager;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * expandable recyclerView adapter
 * @Package com.estrongs.android.pop.app.analysis.adapters
 * @ClassName: ExpandableAdapter
 * @author mayinquan
 * @mail mayinquan@baidu.com
 * @date 2015年11月25日 下午3:15:51
 * @param <G> group data object
 * @param <C> item data object
 */
public abstract class ExpandableAdapter<G, C> extends Adapter<ViewHolder> {
	private final String TAG = "ExpandableAdapter";
	public static int VIEW_TYPE_GROUP = 0;
	public static int VIEW_TYPE_ITEM = 1;
	protected Context mContext;
	private Map<GroupData, CopyOnWriteArrayList<ChildData>> mAllDataMap;
	private CopyOnWriteArrayList<AbsItemData> mAllDataList;
	private OnItemClickListener<G, C> mOnItemClickListener;
	private boolean mIsExpandable = true;
	private RecyclerView mRecyclerView;
	private FrameLayout mCurrentParentView;
	private RecyclerView.OnScrollListener mOnScrollListener;
	
	private boolean mIsSupportStickyHeader = true;
	private View mStickyHeaderView;
	private ViewHolder mStickyHeaderViewHolder;
	private LinearLayoutManager mLinearLayoutManager;
	public ExpandableAdapter(Context context, LinearLayoutManager linearLayoutManager) {
		mAllDataList = new CopyOnWriteArrayList<AbsItemData>();
		mAllDataMap = Collections.synchronizedMap(new LinkedHashMap<GroupData, CopyOnWriteArrayList<ChildData>>());
		this.mContext = context;
		mLinearLayoutManager = linearLayoutManager;
	}

	@Override
	public int getItemCount() {
		return mAllDataList.size();
	}
	
	private AbsItemData getItemData(int position){
		if (position >= 0 && position < mAllDataList.size())
			return mAllDataList.get(position);
		else
			return null;
	}
	
	@Override
	public int getItemViewType(int position) {
		AbsItemData itemData = getItemData(position);
		if(null != itemData && itemData.getClass() == GroupData.class){
			return VIEW_TYPE_GROUP;
		}
		return VIEW_TYPE_ITEM;
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		if (viewType == VIEW_TYPE_GROUP) {
			return onCreateGroupViewHolder(parent);
		} else {
			return onCreateItemViewHolder(parent);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onBindViewHolder(ViewHolder holder, final int position) {
		final AbsItemData data = getItemData(position);
		if (data == null)
			return;
		if (getItemViewType(position) == VIEW_TYPE_GROUP) {
			final GroupData groupData = (GroupData)data;
			onBindGroupHolderData(holder, groupData, groupData.group, groupData.isExpanded);
			holder.itemView.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					if(mIsExpandable){
						if (groupData.isExpanded) {
							collapseGroup(groupData);
						} else {
							expandGroup(groupData);
						}
					}
					if(mIsSupportStickyHeader && null != mLinearLayoutManager){
						onCreateStickyHeaderView();
					}
					if (null != mOnItemClickListener) {
						mOnItemClickListener.onGroupClicked(position, groupData, groupData.group);
					}
				}
			});
		} else {
			final ChildData childData = (ChildData)data;
			onBindItemHolderData(holder, childData, childData.child);
			holder.itemView.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					if (null != mOnItemClickListener) {
						mOnItemClickListener.onItemClicked(position, childData, childData.child);
					}
				}
			});
		}
	}

	public void setOnItemClickListener(OnItemClickListener<G, C> listener) {
		mOnItemClickListener = listener;
	}

	private GroupData addItemData(G group, List<C> children){
		GroupData groupData = new GroupData();
		groupData.isExpanded = false;
		groupData.displayPosition = mAllDataList.size();
		groupData.group = group;
		CopyOnWriteArrayList<ChildData> childrenAbsList = new CopyOnWriteArrayList<ChildData>();
		for (C child : children) {
			ChildData childData = new ChildData();
			childData.child = child;
			childData.baseGroupData = groupData;
			childrenAbsList.add(childData);
		}
		mAllDataMap.put(groupData, childrenAbsList);
		mAllDataList.add(groupData);
		return groupData;
	}
	
	public void addData(G group, List<C> children) {
		GroupData groupData = addItemData(group, children);
		notifyItemInserted(groupData.displayPosition);
	}

	public void setData(Map<G, List<C>> dataMap) {
		mAllDataMap.clear();
		mAllDataList.clear();
		if(null != dataMap){
			for (G group : dataMap.keySet()) {
				addItemData(group, dataMap.get(group));
			}
		}
		notifyDataSetChanged();
	}
	
	@SuppressWarnings("unchecked")
	public void remove(BaseGroupData data){
		int index = mAllDataList.indexOf(data);
		if(index != -1){
			collapseGroup((GroupData)data);
			mAllDataList.remove(index);
			notifyItemRemoved(index);
			notifyPreAndLateData(index);
		}
	}
	
	private void notifyPreAndLateData(int index){
		if(index - 1 >= 0){
			notifyItemChanged(index - 1);
		}
		if(index + 1 < getItemCount()){
			notifyItemChanged(index + 1);
		}
	}
	
	public void remove(BaseChildData data){
		if(null != data){
			BaseGroupData group = data.baseGroupData;
			CopyOnWriteArrayList<ChildData> children = mAllDataMap.get(group);
			children.remove(data);
			int index = mAllDataList.indexOf(data);
			if(index != -1){
				mAllDataList.remove(index);
				notifyItemRemoved(index);
				notifyPreAndLateData(index);
			}
		}
	}

	private void expandGroup(GroupData group) {
		CopyOnWriteArrayList<ChildData> children = mAllDataMap.get(group);
		group.displayPosition = mAllDataList.indexOf(group);
		int groupDisplayPosition = group.displayPosition;
		int itemCount = children.size();
		if (itemCount > 0) {
			int childDisplayPositionStart = groupDisplayPosition + 1;
			for (int index = 0; index < itemCount; index++) {
				children.get(index).displayPosition = childDisplayPositionStart++;
				mAllDataList.add(children.get(index).displayPosition, children.get(index));
			}
			for (int index = groupDisplayPosition + itemCount; index < mAllDataList.size(); index++) {
				mAllDataList.get(index).displayPosition = index;
			}
			notifyItemRangeInserted(groupDisplayPosition + 1, itemCount);
			group.isExpanded = true;
			notifyItemChanged(groupDisplayPosition);
		}
	}

	private void collapseGroup(GroupData group) {
		CopyOnWriteArrayList<ChildData> children = mAllDataMap.get(group);
		group.displayPosition = mAllDataList.indexOf(group);
		int groupDisplayPosition = group.displayPosition;
		int itemCount = children.size();
		if (itemCount > 0) {
			mAllDataList.removeAll(children);
			for (int index = groupDisplayPosition + 1; index < mAllDataList.size(); index++) {
				mAllDataList.get(index).displayPosition = index;
			}
			notifyItemRangeRemoved(groupDisplayPosition + 1, itemCount);
			group.isExpanded = false;
			notifyItemChanged(groupDisplayPosition);
		}
	}
	
	protected void expandGroup(G group){
		Set<GroupData> allGroups = mAllDataMap.keySet();
		for(GroupData groupData : allGroups){
			if(groupData.group == group){
				expandGroup(group);
				return;
			}
		}
	}
	
	protected void collapseGroup(G group){
		Set<GroupData> allGroups = mAllDataMap.keySet();
		for(GroupData groupData : allGroups){
			if(groupData.group == group){
				collapseGroup(group);
				return;
			}
		}
	}
	
	public void collapseAll() {
		for (GroupData group : mAllDataMap.keySet()) {
			collapseGroup(group);
		}
	}

	public void ExpandAll() {
		for (GroupData group : mAllDataMap.keySet()) {
			expandGroup(group);
		}
	}
	
	/**
	 * 是否支持group收缩
	 * @Title: isExpandable
	 * @return
	 * @return boolean
	 * @date 2015年11月25日 下午3:11:28
	 */
	public boolean isExpandable(){
		return mIsExpandable;
	}
	
	/**
	 * 设置group是否收缩
	 * @Title: setExpandable
	 * @param enable
	 * @return void
	 * @date 2015年11月25日 下午3:12:00
	 */
	public void setExpandable(boolean enable){
		mIsExpandable = enable;
	}
	
	/**
	 * 创建GroupViewHolder
	 * @Title: onCreateGroupViewHolder
	 * @param parent
	 * @return
	 * @return ViewHolder
	 * @date 2015年11月25日 下午3:14:11
	 */
	public abstract ViewHolder onCreateGroupViewHolder(ViewGroup parent);
	/**
	 * 创建ItemViewHolder
	 * @Title: onCreateItemViewHolder
	 * @param parent
	 * @return
	 * @return ViewHolder
	 * @date 2015年11月25日 下午3:14:33
	 */
	public abstract ViewHolder onCreateItemViewHolder(ViewGroup parent);
	
	/**
	 * 绑定GroupView数据
	 * @Title: onBindGroupHolderData
	 * @param holder
	 * @param group
	 * @param isExpanded
	 * @return void
	 * @date 2015年11月25日 下午3:14:49
	 */
	public abstract void onBindGroupHolderData(ViewHolder holder, BaseGroupData parent, G group, boolean isExpanded);
	/**
	 * 绑定ItemView数据
	 * @Title: onBindItemHolderData
	 * @param holder
	 * @param child
	 * @return void
	 * @date 2015年11月25日 下午3:15:06
	 */
	public abstract void onBindItemHolderData(ViewHolder holder, BaseChildData parent, C child);
	
	/**
	 * 绑定StickyHeader数据
	 * @Title: onBindStickyHeaderViewHolderData
	 * @param holder groupViewHolder
	 * @return void
	 * @date 2015年12月17日 下午1:45:27
	 */
	public abstract void onBindStickyHeaderViewHolderData(ViewHolder holder);
	
	public static interface OnItemClickListener<G, C> {
		void onGroupClicked(int position, BaseGroupData parent, G group);

		void onItemClicked(int position, BaseChildData parent, C child);
	}

	private static abstract class AbsItemData {
		public int displayPosition;
	}
	
	public static class BaseGroupData extends AbsItemData{
		
	}
	public static class BaseChildData extends AbsItemData{
		public BaseGroupData baseGroupData;
	}
	
	private class ChildData extends BaseChildData{
		public C child;
	}
	
	private class GroupData extends BaseGroupData{
		public boolean isExpanded;
		public G group;
	}
	
	@Override
	public void onAttachedToRecyclerView(RecyclerView recyclerView) {
		super.onAttachedToRecyclerView(recyclerView);
		mRecyclerView = recyclerView;
		if(null != mRecyclerView){
			mRecyclerView.setLayoutManager(mLinearLayoutManager);
		}
		if(!mIsSupportStickyHeader){
			return;
		}
		ViewGroup parent = (ViewGroup) mRecyclerView.getParent();
		mCurrentParentView = new FrameLayout(mContext);
		parent.removeView(mRecyclerView);
		mCurrentParentView.addView(mRecyclerView, mRecyclerView.getLayoutParams());
		parent.addView(mCurrentParentView, mRecyclerView.getLayoutParams());
		mOnScrollListener = new RecyclerView.OnScrollListener() {
			@Override
			public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
				super.onScrolled(recyclerView, dx, dy);
				if(null == mLinearLayoutManager){
					LayoutManager mLayoutManager = mRecyclerView.getLayoutManager();
					if(mLayoutManager instanceof LinearLayoutManager){
						mLinearLayoutManager = (LinearLayoutManager)mLayoutManager;
					}
				}
				if(null != mLinearLayoutManager){
					onCreateStickyHeaderView(dy);
				}
			}
		};
		mRecyclerView.addOnScrollListener(mOnScrollListener);
		getStickyHeaderViewHolder();
	}
	
	@SuppressWarnings("unchecked")
	public Object getItem(int position){
		AbsItemData absItemData = mAllDataList.get(position);
		if(absItemData instanceof BaseGroupData){
			return ((GroupData)absItemData).group;
		}else{
			return ((ChildData)absItemData).child;
		}
	}
	
	public void setSupportStickyHeader(boolean support){
		mIsSupportStickyHeader = support;
	}
	
	/**
	 * 获取置顶headerview
	 * @Title: getStickyHeaderViewHolder
	 * @return
	 * @return ViewHolder
	 * @date 2015年12月21日 下午3:43:27
	 */
	private ViewHolder getStickyHeaderViewHolder(){
		if(mStickyHeaderView == null || null == mStickyHeaderViewHolder){
			mStickyHeaderViewHolder = onCreateGroupViewHolder(null);
			mStickyHeaderView = mStickyHeaderViewHolder.itemView;
			FrameLayout.LayoutParams stickyHeaderViewLp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
					FrameLayout.LayoutParams.WRAP_CONTENT);
			if(null != mStickyHeaderView){
				mCurrentParentView.removeView(mStickyHeaderView);
			}
			mCurrentParentView.addView(mStickyHeaderView, stickyHeaderViewLp);
			mStickyHeaderView.bringToFront();
			mStickyHeaderView.setVisibility(View.GONE);
		}
		return mStickyHeaderViewHolder;
	}
	
	/**
	 * 为headerview 绑定数据
	 * @Title: onBindStickyHeaderViewData
	 * @param parent
	 * @param group
	 * @param isExpanded
	 * @return void
	 * @date 2015年12月21日 下午3:43:43
	 */
	private void onBindStickyHeaderViewData(final BaseGroupData parent, final G group, final boolean isExpanded){
		final ViewHolder stickyHeaderViewHolder = getStickyHeaderViewHolder();
		onBindGroupHolderData(stickyHeaderViewHolder, parent, group, isExpanded);
		stickyHeaderViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				@SuppressWarnings("unchecked")
				GroupData groupData = (GroupData)parent;
				int position = mAllDataList.indexOf(groupData);
				if(mIsExpandable){
					if (groupData.isExpanded) {
						collapseGroup(groupData);
					} else {
						expandGroup(groupData);
					}
				}
				if (null != mOnItemClickListener) {
					mOnItemClickListener.onGroupClicked(position, groupData, groupData.group);
				}
				mLinearLayoutManager.scrollToPosition(position);
			}
		});
		onBindStickyHeaderViewHolderData(stickyHeaderViewHolder);
	}
	
	/**
	 * 创建headerview
	 * @Title: onCreateStickyHeaderView
	 * @return void
	 * @date 2015年12月21日 下午3:44:00
	 */
	@SuppressWarnings("unchecked")
	private void onCreateStickyHeaderView(){
		int position = mLinearLayoutManager.findFirstVisibleItemPosition();
		AbsItemData absItemData = getItemData(position);
		if(null == absItemData){
			if(null != mStickyHeaderView)
				mStickyHeaderView.setVisibility(View.GONE);
			return;
		}
		GroupData groupData = null;
		if(absItemData instanceof BaseGroupData){
			groupData = ((GroupData)absItemData);
		}else{
			groupData = (GroupData)(((BaseChildData)absItemData).baseGroupData);
		}
		G group = groupData.group;
		onBindStickyHeaderViewData(groupData, group, groupData.isExpanded);
	}
	/**
	 * 显示或者移动headerview
	 * @Title: onCreateStickyHeaderView
	 * @param dy
	 * @return void
	 * @date 2015年12月21日 下午3:44:27
	 */
	@SuppressWarnings("unchecked")
	private void onCreateStickyHeaderView(int dy){
		int childCount = mRecyclerView.getChildCount();
		if(childCount >= 2){
            final View firstChild = mRecyclerView.getChildAt(0);
            final View secondChild = mRecyclerView.getChildAt(1);
            final int firstChildPosition = mRecyclerView.getChildAdapterPosition(firstChild);
            final int secondChildPosition = firstChildPosition + 1;
            if (firstChildPosition != RecyclerView.NO_POSITION){
            	AbsItemData firstAbsItemData = getItemData(firstChildPosition);
            	GroupData firstGroupData = null;
        		boolean curIsGroup = false;
        		if(firstAbsItemData instanceof BaseGroupData){
        			firstGroupData = ((GroupData)firstAbsItemData);
        			curIsGroup = true;
        		}else{
        			firstGroupData = (GroupData)(((BaseChildData)firstAbsItemData).baseGroupData);
        			curIsGroup = false;
        		}
        		//如果第一个view是打开情况的group，那么显示headerview
        		if(curIsGroup){
        			mStickyHeaderView.setVisibility(firstGroupData.isExpanded ? View.VISIBLE:View.GONE);
        			mStickyHeaderView.bringToFront();
        			ViewCompat.setTranslationY(mStickyHeaderView, 0);
        		}else{
        			if(secondChildPosition < getItemCount()){
        				AbsItemData secondAbsItemData = getItemData(secondChildPosition);
        				//如果第二个view也是group，那么显示headerview，然后通过secondview 的 y来计算出headerview的位置
        				if(secondAbsItemData instanceof BaseGroupData){
        					mStickyHeaderView.setVisibility(View.VISIBLE);
        					mStickyHeaderView.bringToFront();
        					int decorationH = mLinearLayoutManager.getBottomDecorationHeight(firstChild);
        					float secondChildViewY = ViewCompat.getY(secondChild);
        					int headerViewH = mStickyHeaderView.getHeight();
        					ViewCompat.setTranslationY(mStickyHeaderView, (secondChildViewY - headerViewH - decorationH));
        				}else{//如果第二个view不是group，那么保持目前的headerview
        					mStickyHeaderView.setVisibility(View.VISIBLE);
        					mStickyHeaderView.bringToFront();
        					ViewCompat.setTranslationY(mStickyHeaderView, 0);
        				}
        			}else{
        				mStickyHeaderView.setVisibility(View.VISIBLE);
        				mStickyHeaderView.bringToFront();
        				ViewCompat.setTranslationY(mStickyHeaderView, 0);
        			}
        		}
        		onBindStickyHeaderViewData(firstGroupData, firstGroupData.group, firstGroupData.isExpanded);
            }
        
		}
	}
	
}
