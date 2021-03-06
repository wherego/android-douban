package com.liudong.douban.ui.fragment.member;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.FrameLayout;

import com.liudong.douban.R;
import com.liudong.douban.ui.fragment.LazyFragment;

import butterknife.BindView;

public class BookHistoryFragment extends LazyFragment {

    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    @BindView(R.id.swipeRefresh)
    SwipeRefreshLayout swipeRefresh;
    @BindView(R.id.fl_content)
    FrameLayout flContent;

    public static BookHistoryFragment newInstance() {
        return new BookHistoryFragment();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    protected int getFragmentLayout() {
        return R.layout.fragment_collect_book;
    }

    @Override
    protected void initData() {

    }
}
