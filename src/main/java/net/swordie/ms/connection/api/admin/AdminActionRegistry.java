package net.swordie.ms.connection.api.admin;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherJson;
import net.swordie.ms.connection.api.launcher.LauncherResult;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * {@code action} name -> {@link AdminAction} of {@code /api/v2/admin}. {@link #standard()} is the server's table, one
 * group per console tab (API monitor, accounts, news, daily gift, connection security, saves / shutdown).
 */
public final class AdminActionRegistry {
    private static final Pattern NAME = Pattern.compile("[a-z][a-z0-9]{0,23}\\.[a-z][A-Za-z0-9]{0,31}");

    private final Map<String, AdminAction> actions;

    private AdminActionRegistry(Map<String, AdminAction> actions) {
        this.actions = Collections.unmodifiableMap(actions);
    }

    public static AdminActionRegistry standard() {
        return builder()
                .register(new ServerPingAction())
                // API 監看
                .register(new ApiStatusAction())
                .register(new ApiRevokeSessionsAction())
                // 帳號
                .register(new AccountFindAction())
                .register(new AccountUnlockAction())
                .register(new AccountBanAction())
                .register(new AccountUnbanAction())
                // 公告
                .register(new NewsGetAction())
                .register(new NewsValidateAction())
                .register(new NewsSaveAction())
                // 簽到
                .register(new DailyRewardsAdminAction())
                .register(new DailySaveAdminAction())
                .register(new DailyStatusAdminAction())
                // 連線安全
                .register(new NetGetAction())
                .register(new NetSetAction())
                // 存檔／停機
                .register(new SaveStatusAction())
                .register(new ServerShutdownAction())
                .register(new ServerShutdownCancelAction())
                // 全服公告（notice-1005：LP04F0 系統聊天，--kind 為 GM 實測用）
                .register(new NoticeSendAction())
                // 發佈（登入器更新：release.json、releases\、manifestMeta）
                .register(new ReleaseGetAction())
                .register(new ReleaseValidateAction())
                .register(new ReleaseSaveAction())
                .register(new ReleaseImportAction())
                // 發佈 → 客戶端檔案（1.2.3：client-sign 的輸出匯入、停止發佈）
                .register(new ClientFilesAdmin.Get())
                .register(new ClientFilesAdmin.Import())
                .register(new ClientFilesAdmin.Disable())
                // 禮包序號（P7：建立、列表、停用、兌換紀錄、待發放佇列、道具查詢）
                .register(new GiftAdmin.Create())
                .register(new GiftAdmin.ListCodes())
                .register(new GiftAdmin.Disable())
                .register(new GiftAdmin.Redemptions())
                .register(new GiftAdmin.Queue())
                .register(new GiftAdmin.ItemSearch())
                // 新手流程（step 7：目前設定、各角色進度、重新載入設定）
                .register(new NewbieAdmin.Get())
                .register(new NewbieAdmin.Progress())
                .register(new NewbieAdmin.Reload())
                .register(new NewbieAdmin.Jobs())
                // 上線獎勵（fixes10：設定、各帳號今日分鐘與點數、最近發放紀錄；修改設定）
                .register(new OnlineRewardAdmin.Get())
                .register(new OnlineRewardAdmin.Save())
                // 營運（ops-1007：在線玩家、伺服器健康、技能問題、角色檢視與修補）
                .register(new OpsAdmin.Online())
                .register(new OpsAdmin.Kick())
                .register(new OpsAdmin.Health())
                .register(new OpsAdmin.Issues())
                .register(new OpsAdmin.IssuesClear())
                .register(new OpsAdmin.Inspect())
                .register(new OpsAdmin.Home())
                .register(new OpsAdmin.Cooldowns())
                // launcher 1.2.8: planned restart shown in the launcher, and the players' problem reports
                .register(new ReportAdmin.RestartNotice())
                .register(new ReportAdmin.Reports())
                .register(new ReportAdmin.Report())
                // character kit: an official character's look and gear onto an online character
                .register(new KitAdmin.Kit())
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Optional<AdminAction> find(String name) {
        return Optional.ofNullable(actions.get(name));
    }

    public List<String> names() {
        return List.copyOf(actions.keySet());
    }

    /** Unknown action -> invalidInput; otherwise the action's result. */
    public LauncherResult dispatch(AdminContext ctx, JsonNode request) throws Exception {
        AdminAction action = actions.get(LauncherJson.text(request, "action"));
        if (action == null) {
            return LauncherResult.fail(AdminCodes.INVALID_INPUT).with("problem", "action");
        }
        return action.execute(ctx, request);
    }

    public static final class Builder {
        private final Map<String, AdminAction> actions = new LinkedHashMap<>();

        private Builder() {
        }

        public Builder register(AdminAction action) {
            Objects.requireNonNull(action, "action");
            String name = action.name();
            if (name == null || !NAME.matcher(name).matches()) {
                throw new IllegalArgumentException("invalid admin action name " + name);
            }
            if (actions.containsKey(name)) {
                throw new IllegalArgumentException("duplicate admin action " + name);
            }
            actions.put(name, action);
            return this;
        }

        public AdminActionRegistry build() {
            return new AdminActionRegistry(new LinkedHashMap<>(actions));
        }
    }
}
