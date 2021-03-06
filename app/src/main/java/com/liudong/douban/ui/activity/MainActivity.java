package com.liudong.douban.ui.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.liudong.douban.MyApplication;
import com.liudong.douban.R;
import com.liudong.douban.data.DataManager;
import com.liudong.douban.data.model.user.MovieCollect;
import com.liudong.douban.data.model.user.Person;
import com.liudong.douban.di.components.ActivityComponent;
import com.liudong.douban.event.LogoutEvent;
import com.liudong.douban.event.RxBus;
import com.liudong.douban.ui.fragment.book.BookFragment;
import com.liudong.douban.ui.fragment.movie.MovieFragment;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import cn.bmob.v3.Bmob;
import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.FindListener;
import de.hdodenhof.circleimageview.CircleImageView;
import rx.Subscription;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {

    @BindView(R.id.drawer_layout)
    DrawerLayout drawer;
    @BindView(R.id.nav_view)
    NavigationView navigationView;
    @Inject
    MainDisplay mainDisplay;
    @Inject
    DataManager dataManager;
    @Inject
    MyApplication mApplication;
    @Inject
    RxBus rxBus;

    private View headerView;
    private CircleImageView iv_avatar;
    private TextView tv_name;
    private TextView tv_dec;

    private long mBackPressedTime;
    private CompositeSubscription mCompositeSubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        headerView = navigationView.getHeaderView(0);
        iv_avatar = (CircleImageView) headerView.findViewById(R.id.iv_avatar);
        tv_name = (TextView) headerView.findViewById(R.id.tv_name);
        tv_dec = (TextView) headerView.findViewById(R.id.tv_dec);

        if (savedInstanceState == null) {
            getSupportActionBar().setTitle(R.string.movie_tit);
            navigationView.setCheckedItem(R.id.nav_movie);
            mainDisplay.loadRootFragment(MovieFragment.newInstance(), "MovieFragment");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            drawer.setFitsSystemWindows(true);
            drawer.setClipToPadding(false);
        }
        initLoginState();
        pullCollect();
        eventBus();
    }

    private void initLoginState() {
        Bmob.initialize(this, "ca3beeb6c325e061d5c2e65324e9be5f");
        Person person = BmobUser.getCurrentUser(Person.class);
        if (person != null) {
            mApplication.setLogin(true);
            tv_name.setText(person.getUsername());
            if (person.getPicture() != null) {
                Glide.with(this)
                        .load(person.getPicture())
                        .into(iv_avatar);
            }
            if (person.getDescription() != null) {
                tv_dec.setText(person.getDescription());
            } else {
                tv_dec.setText("这个人很懒，什么都没有留下");
            }
        }
    }

    /**
     * 拉取用户收藏信息
     */
    private void pullCollect() {
        if (mApplication.isLogin()) {
            BmobQuery<MovieCollect> bmobQuery = new BmobQuery<>();
            bmobQuery.addWhereEqualTo("user", BmobUser.getCurrentUser().getUsername());
            bmobQuery.addQueryKeys("objectId,id");
            addSubscription(bmobQuery.findObjects(new FindListener<MovieCollect>() {
                @Override
                public void done(List<MovieCollect> list, BmobException e) {
                    if (e == null) {
                        dataManager.getDataBaseHelper().insertMovieList(list);
                    } else {
                        showToast("用户收藏信息读取失败");
                    }
                }
            }));
        }
    }

    private void eventBus() {
        addSubscription(rxBus.filteredObservable(Person.class)
                .subscribe(new Action1<Person>() {
                    @Override
                    public void call(Person person) {
                        //捕捉异常，防止发生错误时调用error方法导致订阅取消，后面事务无法接收
                        try {
                            mApplication.setLogin(true);
                            tv_name.setText(person.getUsername());
                            if (person.getPicture() != null) {
                                Glide.with(MainActivity.this)
                                        .load(person.getPicture())
                                        .into(iv_avatar);
                            }
                            if (person.getDescription() != null) {
                                tv_dec.setText(person.getDescription());
                            } else {
                                tv_dec.setText("这个人很懒，什么都没有留下");
                            }
                            pullCollect();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }));
        addSubscription(rxBus.filteredObservable(LogoutEvent.class)
                .subscribe(new Action1<LogoutEvent>() {
                    @Override
                    public void call(LogoutEvent logoutEvent) {
                        try {
                            mApplication.setLogin(logoutEvent.isLogin());
                            iv_avatar.setImageResource(R.mipmap.avatar_anynomous);
                            tv_name.setText(logoutEvent.getUsername());
                            tv_dec.setText(logoutEvent.getDec());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }));
    }

    /**
     * 切换主题设置
     */
    private void toggleThemeSetting(MenuItem item) {
        //展示动画
        showAnimation();

        boolean isNight = sp.getBoolean("night", false);
        if (isNight) {
            setTheme(R.style.AppTheme);
            sp.edit().putBoolean("night", false).apply();
            item.setTitle("夜间模式");
        } else {
            setTheme(R.style.NightTheme);
            sp.edit().putBoolean("night", true).apply();
            item.setTitle("白天模式");
        }

        //刷新UI
        refreshUI();
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void refreshUI() {
        TypedValue toolbarBg = new TypedValue();
        TypedValue mainBg = new TypedValue();
        TypedValue cardViewBg = new TypedValue();
        TypedValue cardViewTit = new TypedValue();
        Resources.Theme theme = getTheme();
        theme.resolveAttribute(R.attr.colorPrimary, toolbarBg, true);
        theme.resolveAttribute(R.attr.mainBackground, mainBg, true);
        theme.resolveAttribute(R.attr.cardViewBackground, cardViewBg, true);
        theme.resolveAttribute(R.attr.cardViewTit, cardViewTit, true);

        toolbar.setBackgroundResource(toolbarBg.resourceId);
        headerView.setBackgroundResource(toolbarBg.resourceId);

        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        for (Fragment fragment : fragments) {
            if (fragment instanceof MovieFragment) {
                fragment.getView().findViewById(R.id.tab_content).setBackgroundResource(toolbarBg.resourceId);
                List<Fragment> childFragments = fragment.getChildFragmentManager().getFragments();
                for (Fragment childFragment : childFragments) {
                    View view = childFragment.getView();
                    view.findViewById(R.id.fl_content).setBackgroundResource(mainBg.resourceId);
                    RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
                    int childCount = recyclerView.getChildCount();
                    for (int childIndex = 0; childIndex < childCount; childIndex++) {
                        View childView = recyclerView.getChildAt(childIndex);
                        if (childView instanceof CardView) {
                            TextView tv_title = (TextView) childView.findViewById(R.id.tv_title);
                            //注意CardView设置背景问题http://blog.9i0i.com/?u013290250/article/details/51211537
                            ((CardView) childView).setCardBackgroundColor(getResources().getColor(cardViewBg.resourceId));
                            tv_title.setTextColor(getResources().getColor(cardViewTit.resourceId));
                        }
                    }

                    //让RecyclerView缓存在Pool中的Item失效
                    try {
                        Field declaredField = RecyclerView.class.getDeclaredField("mRecycler");
                        declaredField.setAccessible(true);
                        Method declaredMethod = Class.forName(RecyclerView.Recycler.class.getName()).getDeclaredMethod("clear", (Class<?>[]) new Class[0]);
                        declaredMethod.setAccessible(true);
                        declaredMethod.invoke(declaredField.get(recyclerView));
                        RecyclerView.RecycledViewPool recycledViewPool = recyclerView.getRecycledViewPool();
                        recycledViewPool.clear();

                    } catch (NoSuchFieldException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 展示一个切换动画
     */
    private void showAnimation() {
        final View decorView = getWindow().getDecorView();
        Bitmap cacheBitmap = getCacheBitmapFromView(decorView);
        if (decorView instanceof ViewGroup && cacheBitmap != null) {
            final View view = new View(this);
            view.setBackgroundDrawable(new BitmapDrawable(getResources(), cacheBitmap));
            ViewGroup.LayoutParams layoutParam = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            ((ViewGroup) decorView).addView(view, layoutParam);
            ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f);
            objectAnimator.setDuration(300);
            objectAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    ((ViewGroup) decorView).removeView(view);
                }
            });
            objectAnimator.start();
        }
    }

    /**
     * 获取一个View的缓存视图
     */
    private Bitmap getCacheBitmapFromView(View view) {
        final boolean drawingCacheEnabled = true;
        view.setDrawingCacheEnabled(drawingCacheEnabled);
        view.buildDrawingCache(drawingCacheEnabled);
        final Bitmap drawingCache = view.getDrawingCache();
        Bitmap bitmap;
        if (drawingCache != null) {
            bitmap = Bitmap.createBitmap(drawingCache);
            view.setDrawingCacheEnabled(false);
        } else {
            bitmap = null;
        }
        return bitmap;
    }

    private void addSubscription(Subscription s) {
        if (mCompositeSubscription == null) {
            mCompositeSubscription = new CompositeSubscription();
        }
        mCompositeSubscription.add(s);
    }

    @Override
    protected int getContentViewID() {
        return R.layout.activity_main;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCompositeSubscription != null) {
            mCompositeSubscription.unsubscribe();
        }
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            long curTime = SystemClock.uptimeMillis();
            if ((curTime - mBackPressedTime) < (3 * 1000)) {
                finish();
            } else {
                mBackPressedTime = curTime;
                showToast(getString(R.string.tip_double_click_exit));
            }
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.nav_movie:
                MovieFragment movieFragment = (MovieFragment) getSupportFragmentManager().findFragmentByTag("MovieFragment");
                if (movieFragment == null) {
                    movieFragment = MovieFragment.newInstance();
                    mainDisplay.addFragmentToActivity(movieFragment, "MovieFragment");
                }
                mainDisplay.showFragment(movieFragment);
                getSupportActionBar().setTitle(R.string.movie_tit);
                break;
            case R.id.nav_book:
                BookFragment bookFragment = (BookFragment) getSupportFragmentManager().findFragmentByTag("BookFragment");
                if (bookFragment == null) {
                    bookFragment = BookFragment.newInstance();
                    mainDisplay.addFragmentToActivity(bookFragment, "BookFragment");
                }
                mainDisplay.showFragment(bookFragment);
                getSupportActionBar().setTitle(R.string.book_tit);
                break;
            case R.id.me:
                if (mApplication.isLogin()) {
                    startActivity(new Intent(this, MemberActivity.class));
                } else {
                    startActivity(new Intent(this, LoginActivity.class));
                }
                break;
            case R.id.about:
                break;
        }
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem item = menu.findItem(R.id.action_theme);
        if (isNight) {
            item.setTitle("白天模式");
        } else {
            item.setTitle("夜间模式");
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
            case R.id.action_theme:
                toggleThemeSetting(item);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void injectDagger(ActivityComponent activityComponent) {
        activityComponent.inject(this);
    }
}
