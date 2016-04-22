package com.dmd.zsb.teacher.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.mobileim.YWChannel;
import com.alibaba.mobileim.YWIMKit;
import com.alibaba.mobileim.channel.event.IWxCallback;
import com.alibaba.mobileim.channel.util.YWLog;
import com.alibaba.mobileim.login.YWLoginCode;
import com.alibaba.mobileim.login.YWLoginState;
import com.dmd.tutor.eventbus.EventCenter;
import com.dmd.tutor.netstatus.NetUtils;
import com.dmd.tutor.utils.CommonUtils;
import com.dmd.tutor.utils.XmlDB;
import com.dmd.tutor.widgets.ProgressDialog;
import com.dmd.zsb.common.Constants;
import com.dmd.zsb.openim.LoginSampleHelper;
import com.dmd.zsb.openim.Notification;
import com.dmd.zsb.openim.NotificationInitSampleHelper;
import com.dmd.zsb.openim.UserProfileSampleHelper;
import com.dmd.zsb.teacher.R;
import com.dmd.zsb.mvp.presenter.impl.SignInPresenterImpl;
import com.dmd.zsb.mvp.view.SignInView;
import com.dmd.zsb.teacher.activity.base.BaseActivity;
import com.dmd.zsb.widgets.ToastView;
import com.google.gson.JsonObject;
import com.squareup.otto.Subscribe;
import butterknife.Bind;

public class SignInActivity extends BaseActivity implements SignInView, View.OnClickListener {
    private static final String TAG=SignInActivity.class.getSimpleName();
    @Bind(R.id.et_mobile)
    EditText etMobile;
    @Bind(R.id.et_password)
    EditText etPassword;
    @Bind(R.id.btn_login)
    Button btnLogin;
    @Bind(R.id.tv_signup)
    TextView tvSignup;

    private SignInPresenterImpl signInPresenter;

    private static final String MOBILE = "mobile";
    private static final String PASSWORD = "password";

    public static final String AUTO_LOGIN_STATE_ACTION = "com.dmd.zsb.parent.autoLoginStateActionn";
    private ProgressDialog progressDialog=null;



    private BroadcastReceiver mAutoLoginStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final int state = intent.getIntExtra("state", -1);
            YWLog.i(TAG, "mAutoLoginStateReceiver, loginState = " + state);
            if (state == -1) {
                return;
            }
            handleAutoLoginState(state);
        }
    };
    @Override
    protected void getBundleExtras(Bundle extras) {

    }

    @Override
    protected int getContentViewLayoutID() {
        return R.layout.activity_sign_in;
    }

    @Subscribe
    @Override
    public void onEventComming(EventCenter eventCenter) {

    }

    @Override
    protected View getLoadingTargetView() {
        return null;
    }

    @Override
    protected void initViewsAndEvents() {

        //初始化 open im
        init(etMobile.getText().toString(), getString(R.string.app_key));
        myRegisterReceiver();
        signInPresenter = new SignInPresenterImpl(mContext, this);
        etMobile.setOnClickListener(this);
        etPassword.setOnClickListener(this);
        btnLogin.setOnClickListener(this);
        tvSignup.setOnClickListener(this);
        if (!CommonUtils.isEmpty(XmlDB.getInstance(mContext).getKeyString("mobile", "")))
            etMobile.setText(XmlDB.getInstance(mContext).getKeyString("mobile", ""));
        if (!CommonUtils.isEmpty(XmlDB.getInstance(mContext).getKeyString("password", "")))
            etPassword.setText(XmlDB.getInstance(mContext).getKeyString("password", ""));
    }

    @Override
    protected void onNetworkConnected(NetUtils.NetType type) {

    }

    @Override
    protected void onNetworkDisConnected() {

    }

    @Override
    protected boolean isApplyStatusBarTranslucency() {
        return false;
    }

    @Override
    protected boolean isBindEventBusHere() {
        return false;
    }

    @Override
    protected boolean toggleOverridePendingTransition() {
        return false;
    }

    @Override
    protected TransitionMode getOverridePendingTransitionMode() {
        return null;
    }

    @Override
    public void onClick(View v) {

        String mobile = etMobile.getText().toString();
        String password = etPassword.getText().toString();

        switch (v.getId()) {
            case R.id.tv_signup:
                // 打开注册页面
                CloseKeyBoard();
                readyGoThenKill(SignUpActivity.class);
                break;
            case R.id.btn_login:
                if ("".equals(mobile)) {
                    ToastView toast = new ToastView(this, getString(R.string.please_input_mobile_phone));
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    etMobile.requestFocus();
                } else if ("".equals(password)) {
                    ToastView toast = new ToastView(this, getString(R.string.please_input_password));
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    etMobile.requestFocus();
                } else if (mobile.length() < 11) {
                    ToastView toast = new ToastView(this, getString(R.string.wrong_mobile_phone));
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    etMobile.requestFocus();
                } else if (password.length() < 6 || password.length() > 20) {
                    ToastView toast = new ToastView(this, getString(R.string.please_enter_correct_password_format));
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    etPassword.requestFocus();
                } else {
                    CloseKeyBoard();
                    btnLogin.setClickable(false);
                    init(etMobile.getText().toString(), getString(R.string.app_key));
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("appkey", Constants.ZSBAPPKEY);
                    jsonObject.addProperty("version", Constants.ZSBVERSION);
                    jsonObject.addProperty("location", XmlDB.getInstance(mContext).getKeyString("addr","未取得定位地址"));
                    jsonObject.addProperty("lat", XmlDB.getInstance(mContext).getKeyFloatValue("latitude", 0)+"");
                    jsonObject.addProperty("lon", XmlDB.getInstance(mContext).getKeyFloatValue("longitude", 0)+"");
                    jsonObject.addProperty("client_type", Constants.PLATFORM);
                    jsonObject.addProperty("role", Constants.USER_ROLE);
                    jsonObject.addProperty("mobile", mobile);
                    jsonObject.addProperty("password", password);
                    signInPresenter.signIn(jsonObject);

                    progressDialog=new ProgressDialog(mContext,getString(R.string.please_later_on));
                    progressDialog.show();
                }
                break;
        }
    }
    // 关闭键盘
    private void CloseKeyBoard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(etMobile.getWindowToken(), 0);
    }

    @Override
    public void navigateToHome() {

        LoginSampleHelper.getInstance().login_Sample(etMobile.getText().toString(), etPassword.getText().toString(), getString(R.string.app_key), new IWxCallback() {

            @Override
            public void onSuccess(Object... arg0) {
                saveIdPasswordToLocal(etMobile.getText().toString(), etPassword.getText().toString());

                btnLogin.setClickable(true);
                if (progressDialog!=null){
                    progressDialog.dismiss();
                    progressDialog=null;
                }
                Toast.makeText(mContext, "登录成功",Toast.LENGTH_SHORT).show();
                YWLog.i(TAG, "login success!");
                XmlDB.getInstance(mContext).saveKey("isLogin", true);
                Bundle bundle=new Bundle();
                bundle.putString(MainActivity.LOGIN_SUCCESS,"loginSuccess");
                readyGo(MainActivity.class,bundle);
                finish();

            }

            @Override
            public void onProgress(int arg0) {

            }

            @Override
            public void onError(int errorCode, String errorMessage) {
                if (progressDialog!=null){
                    progressDialog.dismiss();
                    progressDialog=null;
                }
                if (errorCode == YWLoginCode.LOGON_FAIL_INVALIDUSER) { //若用户不存在，则提示使用游客方式登陆
                   showTip(errorMessage);
                } else {
                    btnLogin.setClickable(true);
                    YWLog.w(TAG, "登录失败，错误码：" + errorCode + "  错误信息：" + errorMessage);
                    Notification.showToastMsg(mContext, errorMessage);
                }
            }
        });
    }

    @Override
    public void showTip(String msg) {
        if (progressDialog!=null){
            progressDialog.dismiss();
            progressDialog=null;
        }
        ToastView toast = new ToastView(this, msg);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();

    }
    //=================================================open im========================================================
    private void init(String mobile, String appKey) {
        //初始化imkit
        LoginSampleHelper.getInstance().initIMKit(mobile, appKey);
        //自定义头像和昵称回调初始化(如果不需要自定义头像和昵称，则可以省去)
        UserProfileSampleHelper.initProfileCallback();
        //通知栏相关的初始化
        NotificationInitSampleHelper.init();

    }
    /**
     * 保存登录的用户名密码到本地
     *
     * @param mobile
     * @param password
     */
    private void saveIdPasswordToLocal(String mobile, String password) {
        XmlDB.getInstance(mContext).saveKey("mobile", mobile);
        XmlDB.getInstance(mContext).saveKey("password", password);
    }
    @Override
    protected void onResume() {
        super.onResume();
        handleAutoLoginState(LoginSampleHelper.getInstance().getAutoLoginState().getValue());
        YWLog.i(TAG, "onResume, loginState = " + LoginSampleHelper.getInstance().getAutoLoginState().getValue());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressDialog!=null){
            progressDialog.dismiss();
            progressDialog=null;
        }
        myUnregisterReceiver();
    }

    private void myRegisterReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(AUTO_LOGIN_STATE_ACTION);
        LocalBroadcastManager.getInstance(YWChannel.getApplication()).registerReceiver(mAutoLoginStateReceiver, filter);
    }

    private void myUnregisterReceiver() {
        LocalBroadcastManager.getInstance(YWChannel.getApplication()).unregisterReceiver(mAutoLoginStateReceiver);
    }

    private void handleAutoLoginState(int loginState) {

        if (loginState == YWLoginState.logining.getValue()) {
            if (progressDialog==null) {
                progressDialog=new ProgressDialog(mContext,getString(R.string.please_later_on));
                progressDialog.show();
            }
            btnLogin.setClickable(false);
        } else if (loginState == YWLoginState.success.getValue()) {
            btnLogin.setClickable(true);
            if (progressDialog!=null){
                progressDialog.dismiss();
                progressDialog=null;
            }
            readyGoThenKill(MainActivity.class);
        } else {
            YWIMKit ywimKit = LoginSampleHelper.getInstance().getIMKit();
            if (ywimKit != null) {
                if (ywimKit.getIMCore().getLoginState() == YWLoginState.success) {
                    btnLogin.setClickable(true);
                    progressDialog.dismiss();
                    progressDialog=null;
                    readyGoThenKill(MainActivity.class);
                    return;
                }
            }
            //当作失败处理
            btnLogin.setClickable(true);
            if (progressDialog!=null){
                progressDialog.dismiss();
                progressDialog=null;
            }
        }
    }
}
