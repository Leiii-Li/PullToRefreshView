package john.com.pulltorefreshview;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Created by John on 2017/11/27.
 */

public class PullToRefreshView extends ListView {

    private RotateAnimation mUpAnimation;
    private RotateAnimation mDownAnimation;
    private View mHeaderView;
    private int mHeaderViewHeight;
    private ImageView mHeaderImageView;
    private ProgressBar mHeaderProgressBar;
    private TextView mHeaderTextView;

    private final int PULL_DOWN = 1;
    private final int REFRESH = 2;
    private final int REFRESHING = 3;
    private int CURRENT_STATE = 1;

    public PullToRefreshView(Context context) {
        this(context, null);
    }

    public PullToRefreshView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public PullToRefreshView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        initAnimation();
        mHeaderView = View.inflate(getContext(), R.layout.header_view, null);
        mHeaderView.measure(0, 0);
        mHeaderViewHeight = mHeaderView.getMeasuredHeight();
        mHeaderView.setPadding(0, -mHeaderViewHeight, 0, 0);
        this.addHeaderView(mHeaderView);
        initView();
    }

    private void initView() {
        mHeaderImageView = (ImageView) mHeaderView.findViewById(R.id.imageView);
        mHeaderProgressBar = (ProgressBar) mHeaderView.findViewById(R.id.progressBar);
        mHeaderTextView = (TextView) mHeaderView.findViewById(R.id.textView);
    }

    private void initAnimation() {
        // 箭头向上
        mUpAnimation = new RotateAnimation(0, -180, Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        mUpAnimation.setDuration(500);
        mUpAnimation.setFillAfter(true);// 保持动画结束的状态
        // 箭头向下
        mDownAnimation = new RotateAnimation(-180, -360, Animation.RELATIVE_TO_SELF,
                0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        mDownAnimation.setDuration(500);
        mDownAnimation.setFillAfter(true);// 保持动画结束的状态
    }

    private int startY;

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startY = (int) ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                int endY = (int) ev.getY();
                int distance = endY - startY;
                // 可见的第一个布局的下标为 0 且滑动的距离大于0 代表往下拉，说明需要进行逻辑处理，否则是为上滑不用进行逻辑处理
                if (getFirstVisiblePosition() == 0 && distance > 0) {
                    //如果是正在刷新状态，不用进行处理，不能往下拉，但是能够往上滑
                    if (CURRENT_STATE != REFRESHING) {
                        int top = distance - mHeaderViewHeight;
                        if (top > 0 && CURRENT_STATE != REFRESH) {
                            CURRENT_STATE = REFRESH;
                            switchOption();
                        }
                        if (top < 0 && CURRENT_STATE != PULL_DOWN) {
                            CURRENT_STATE = PULL_DOWN;
                            switchOption();
                        }
                        mHeaderView.setPadding(0, top, 0, 0);
                        // 这里必须return true 否则会有问题
                        return true;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (CURRENT_STATE == REFRESH) {
                    CURRENT_STATE = REFRESHING;
                    switchOption();
                    mHeaderView.setPadding(0, 0, 0, 0);
                }
                if (CURRENT_STATE == PULL_DOWN) {
                    mHeaderView.setPadding(0, -mHeaderViewHeight, 0, 0);
                }
                break;
        }
        return super.onTouchEvent(ev);
    }

    private void switchOption() {
        switch (CURRENT_STATE) {
            case PULL_DOWN:
                mHeaderTextView.setText("下拉刷新");
                mHeaderImageView.startAnimation(mDownAnimation);
                break;
            case REFRESH:
                mHeaderTextView.setText("松开刷新");
                mHeaderImageView.startAnimation(mUpAnimation);
                break;
            case REFRESHING:
                mHeaderTextView.setText("刷新中..");
                mHeaderImageView.clearAnimation();
                mHeaderProgressBar.setVisibility(View.VISIBLE);
                mHeaderImageView.setVisibility(View.INVISIBLE);
                doRefresh();
                break;
        }
    }

    private void finish() {
        mHeaderView.setPadding(0, -mHeaderViewHeight, 0, 0);
        CURRENT_STATE = PULL_DOWN;
        mHeaderImageView.setVisibility(View.VISIBLE);
        mHeaderProgressBar.setVisibility(View.INVISIBLE);
        switchOption();
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            finish();
        }
    };

    private void doRefresh() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                SystemClock.sleep(3000);
                mHandler.sendEmptyMessage(0);
            }
        }).start();
    }
}