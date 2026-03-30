package edu.hitsz;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode; // 引入 ActionMode
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.hitsz.ScoreRecord.PlayerScore;
import edu.hitsz.ScoreRecord.ScoreRecords;

public class LeaderBoardActivity extends AppCompatActivity {

    private RadioGroup bottomNavigation, rgDifficultySwitch;
    private RecyclerView rvRankingList;

    // 当前活动的 ActionMode (用于多选删除)
    private ActionMode currentActionMode;


    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable navigationRunnable;

    private final Map<Integer, String> difficultyTagMap = Map.of(
            R.id.rbSimple, "simple",
            R.id.rbMedium, "medium",
            R.id.rbDifficult, "difficult"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        rvRankingList = findViewById(R.id.rvRankingList);
        rgDifficultySwitch = findViewById(R.id.rgDifficultySwitch);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        //添加一键清空
        Button btnReset = findViewById(R.id.btnReset); // 推荐：在 XML 中定义
        if (btnReset != null) {
            btnReset.setText("🗑️ 清空排行榜");
            // 如果是动态创建，需要添加到布局中
            // LinearLayout rootLayout = findViewById(R.id.root_layout); // 你的根布局ID
            // rootLayout.addView(btnReset, 0); // 插入到顶部

            // 【3】设置点击事件
            btnReset.setOnClickListener(v -> {
                // 弹出确认对话框，防止误触
                new AlertDialog.Builder(LeaderBoardActivity.this)
                        .setTitle("警告")
                        .setMessage("确定要清空当前难度的所有记录吗？此操作不可恢复！")
                        .setPositiveButton("清空", (dialog, which) -> {
                            // 获取当前选中的难度
                            int selectedId = rgDifficultySwitch.getCheckedRadioButtonId();
                            String currentTag = difficultyTagMap.get(selectedId);
                            if (currentTag == null) currentTag = "simple";

                            // 执行清空
                            ScoreRecords records = new ScoreRecords(this, currentTag);
                            boolean success = records.clearAllScores();

                            if (success) {
                                // 清空成功后，刷新列表
                                loadAndDisplayRankings(currentTag);
                                Toast.makeText(this, "记录已清空", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "清空失败", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
            });
        }
        rvRankingList.setLayoutManager(new LinearLayoutManager(this));

        // 难度切换
        rgDifficultySwitch.setOnCheckedChangeListener((group, checkedId) -> {
            String tag = difficultyTagMap.get(checkedId);
            if (tag != null) loadAndDisplayRankings(tag);
        });

        // 底部导航
        bottomNavigation.setOnCheckedChangeListener((group, checkedId) -> {
            if (navigationRunnable != null) handler.removeCallbacks(navigationRunnable);
            if (checkedId == R.id.rbHome) {
                navigationRunnable = () -> startActivity(new Intent(LeaderBoardActivity.this, SelectActivity.class));
                handler.postDelayed(navigationRunnable, 200);
            }
        });

        loadAndDisplayRankings("simple");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 获取当前选中的难度
        int selectedId = rgDifficultySwitch.getCheckedRadioButtonId();
        String currentTag = difficultyTagMap.get(selectedId);
        if (currentTag == null) {
            currentTag = "simple"; // 默认值兜底
        }
        // 重新加载数据
        loadAndDisplayRankings(currentTag);
    }
    private void loadAndDisplayRankings(String difficultyTag) {
        ScoreRecords records = new ScoreRecords(this, difficultyTag);
        List<PlayerScore> sortedList = records.getSortedScores();

        // 每次加载新数据时，退出选择模式
        if (currentActionMode != null) {
            currentActionMode.finish();
        }

        RankingAdapter adapter = new RankingAdapter(sortedList, difficultyTag);
        rvRankingList.setAdapter(adapter);
    }

    // --- 适配器内部类 ---
    /**
     * 排行榜适配器 (RankingAdapter)
     * 作用：连接数据(List<PlayerScore>)与UI(R.layout.item_ranking)，负责将分数数据显示在RecyclerView列表中。
     * 特色：支持顶部自定义标题 + 批量删除的多选模式。
     */
    class RankingAdapter extends RecyclerView.Adapter<RankingAdapter.RankingViewHolder> {
        // 【数据源】存储从文件读取的玩家分数列表
        private List<PlayerScore> dataList;
        // 【配置】当前显示的难度标签("simple", "medium", "hard")
        private String currentDifficultyTag;
        // 【状态】多选模式下，用户选中的项目列表
        private List<PlayerScore> selectedItems = new ArrayList<>(); // 存放选中的项
        // 【状态】标志位，true表示正处于多选模式(显示CheckBox)，false表示普通浏览模式
        private boolean isChoiceMode = false; // 是否处于多选模式

        public RankingAdapter(List<PlayerScore> dataList, String difficultyTag) {
            this.dataList = dataList;
            this.currentDifficultyTag = difficultyTag;
        }

        /**
         * 创建ViewHolder (负责加载布局文件)
         * @param parent 父容器
         * @param viewType 视图类型
         * @return 新创建的ViewHolder
         */
        @Override
        public RankingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_ranking, parent, false);
            return new RankingViewHolder(view);
        }
        /**
         * 绑定数据 (核心方法，负责将数据填充到控件上)
         * @param holder 视图持有者
         * @param position 列表中的绝对位置
         */
        @Override
        public void onBindViewHolder(@NonNull RankingViewHolder holder, int position) {
            // 1. 处理标题 (Position 0)
            if (position == 0) {
                holder.bindHeader(getTitleFromTag(currentDifficultyTag));
                return;
            }

            // 2. 处理数据 (Position 1 ~ N)
            int dataPosition = position - 1;
            if (dataPosition >= dataList.size()) return;

            PlayerScore ps = dataList.get(dataPosition);
            // 绑定具体数据：分数对象、排名(从1开始)、是否多选模式、是否被选中
            holder.bindData(ps, dataPosition + 1, isChoiceMode, selectedItems.contains(ps));

            // 3. 点击事件
            holder.itemView.setOnClickListener(v -> {
                if (isChoiceMode) {
                    toggleSelection(ps, holder);
                }
            });

            // 4. 长按事件,进入多选模式
            holder.itemView.setOnLongClickListener(v -> {
                if (!isChoiceMode) {
                    startChoiceMode();
                    toggleSelection(ps, holder); // 自动选中长按项
                }
                return true;
            });
        }

        /**
         * 获取列表总条目数
         * @return 数据条数 + 1 (1代表顶部的标题栏)
         */
        @Override
        public int getItemCount() {
            return dataList.size() + 1;
        }

        // --- 交互逻辑方法 ---

        /**
         * 开启多选模式
         * 功能：调用 Support Library 的 ActionMode，显示顶部的操作栏(包含删除按钮)
         */
        private void startChoiceMode() {
            isChoiceMode = true;
            // 启动 ActionMode，传入自定义回调
            currentActionMode = startSupportActionMode(new ActionMode.Callback() {

                // 1. 创建菜单：这里手动添加“删除”按钮
                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    // 参数：组ID，项ID，顺序，标题
                    menu.add(Menu.NONE, 101, Menu.NONE, "删除选中");
                    return true;
                }

                // 2. 准备菜单（通常留空）
                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    return false;
                }

                // 3. 处理菜单点击：处理删除逻辑
                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    if (item.getItemId() == 101) { // 如果点击的是“删除选中”
                        deleteSelectedItems(); // 调用删除方法
                        return true;
                    }
                    return false;
                }

                // 4. 销毁菜单：退出多选模式
                @Override
                public void onDestroyActionMode(ActionMode mode) {
                    isChoiceMode = false;
                    selectedItems.clear();
                    currentActionMode = null;
                    notifyDataSetChanged(); // 刷新列表，隐藏 CheckBox
                }
            });

            // 设置初始标题
            if (currentActionMode != null) {
                currentActionMode.setTitle("已选择 0 项");
            }
        }

        /**
         * 切换选中状态
         * 功能：将点击的项目加入或移出 selectedItems 列表，并更新UI
         */
        private void toggleSelection(PlayerScore ps, RankingViewHolder holder) {
            if (selectedItems.contains(ps)) {
                selectedItems.remove(ps);
            } else {
                selectedItems.add(ps);
            }
            // 局部刷新：只刷新当前Item的UI状态(选中/未选中)
            // 重新计算排名 dataList.indexOf(ps) + 1
            holder.bindData(ps, dataList.indexOf(ps) + 1, true, selectedItems.contains(ps));

            // 更新顶部标题数量
            if (currentActionMode != null) {
                currentActionMode.setTitle("已选择 " + selectedItems.size() + " 项");
            }
        }

        // 执行删除
        @SuppressLint("NotifyDataSetChanged")
        public void deleteSelectedItems() {
            if (selectedItems.isEmpty()) return;

            // 1. 执行删除（使用上面修复后的方法）
            ScoreRecords records = new ScoreRecords(LeaderBoardActivity.this, currentDifficultyTag);
            for (PlayerScore ps : selectedItems) {
                records.deleteScore(ps);
            }

            // 2. 重新加载数据
            dataList.clear(); // 清空旧列表
            dataList.addAll(records.getSortedScores()); // 从文件读取最新列表

            // 3. 刷新界面
            selectedItems.clear();
            notifyDataSetChanged();

            if (currentActionMode != null) currentActionMode.finish();
            Toast.makeText(LeaderBoardActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
        }

        // --- ViewHolder ---
        class RankingViewHolder extends RecyclerView.ViewHolder {
            TextView tvRank, tvName, tvScore;
            CheckBox cbSelect;

            /**
             * 视图持有者 (RankingViewHolder)
             * 作用：缓存布局文件中的控件ID，负责具体的UI渲染。
             */
            public RankingViewHolder(View itemView) {
                super(itemView);
                tvRank = itemView.findViewById(R.id.tvRank);
                tvName = itemView.findViewById(R.id.tvName);
                tvScore = itemView.findViewById(R.id.tvScore);
                cbSelect = itemView.findViewById(R.id.cbSelect);
            }

            /**
             * 绑定普通数据行
             */
            public void bindData(PlayerScore ps, int rank, boolean choiceMode, boolean isSelected) {
                tvRank.setText(String.valueOf(rank));
                tvName.setText(ps.getPlayerName());
                tvScore.setText(ps.getScore() + "分");
                tvName.setTypeface(null, Typeface.NORMAL);

                // 控制 CheckBox 显示与选中状态
                cbSelect.setVisibility(choiceMode ? View.VISIBLE : View.GONE);
                cbSelect.setChecked(isSelected);

                // 选中时改变背景色
                itemView.setBackgroundColor(isSelected ? 0x200000FF : 0x00000000);
            }

            // 绑定标题
            public void bindHeader(String title) {
                tvName.setText(title);
                tvScore.setText("");
                tvRank.setText("");
                cbSelect.setVisibility(View.GONE);
                tvName.setTypeface(null, Typeface.BOLD);
                itemView.setBackgroundColor(0x00000000);
            }
        }

        private String getTitleFromTag(String tag) {
            if ("simple".equals(tag)) return "【 简单模式 】";
            else if ("medium".equals(tag)) return "【 中等模式 】";
            else return "【 困难模式 】";
        }
    }
}