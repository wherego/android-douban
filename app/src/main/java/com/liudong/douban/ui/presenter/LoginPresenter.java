package com.liudong.douban.ui.presenter;

import com.liudong.douban.data.DataManager;
import com.liudong.douban.data.model.user.Person;

import javax.inject.Inject;

import cn.bmob.v3.BmobUser;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.LogInListener;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

public class LoginPresenter extends Presenter<LoginPresenter.View> {

    private View view;
    private final DataManager mDataManager;

    @Inject
    LoginPresenter(DataManager mDataManager) {
        this.mDataManager = mDataManager;
    }

    @Override
    public void attachView(View view) {
        this.view = view;
    }

    @Override
    public void detachView() {
        unSubscribe();
        view = null;
    }

    public void login(String nameOrNumber, String password) {
        addSubscription(BmobUser.loginByAccount(nameOrNumber, password, new LogInListener<Person>() {
            @Override
            public void done(Person o, BmobException e) {
                if (e == null && o != null) {
                    view.succeed();
                } else {
                    view.showMessage(e.getMessage());
                }
            }
        }));
    }

    public interface View {
        void showProgress();

        void hideProgress();

        void showMessage(String message);

        void succeed();
    }
}
