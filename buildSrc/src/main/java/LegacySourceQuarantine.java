import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

public final class LegacySourceQuarantine {

    /**
     * Authoritative allowlist for the tracked {@code app/src/} tree (verified against disk on
     * every CI run by {@code BuildSupportTest#manifestMatchesEveryFileUnderAppSrc}).
     *
     * <p>Having this pure-JVM baseline means orphan cleanup works identically even when the user
     * updates an older local 1.0.4 folder by extracting GitHub's "Download ZIP" archive (no
     * {@code .git} folder, stale {@code .git} index, {@code git} absent from daemon {@code PATH},
     * or Windows {@code dubious ownership} exit 128).
     */
    public static final Set<String> EXPECTED_APP_SRC_FILES = Collections.unmodifiableSet(
        new TreeSet<>(List.of(
            "androidTest/java/ma/etherealmber/atlascommerce/Android11StartupSmokeTest.kt",
            "debug/AndroidManifest.xml",
            "debug/res/xml/debug_network_security_config.xml",
            "main/AndroidManifest.xml",
            "main/assets/licenses/LICENSE-APACHE.txt",
            "main/assets/logo-dark.png",
            "main/assets/logo-dark.svg",
            "main/assets/logo-light.png",
            "main/assets/logo-light.svg",
            "main/java/ma/etherealmber/atlascommerce/AtlasApp.kt",
            "main/java/ma/etherealmber/atlascommerce/MainActivity.kt",
            "main/java/ma/etherealmber/atlascommerce/core/AppDetails.kt",
            "main/java/ma/etherealmber/atlascommerce/core/AppLanguage.kt",
            "main/java/ma/etherealmber/atlascommerce/core/Appearance.kt",
            "main/java/ma/etherealmber/atlascommerce/core/diagnostics/AnalyticsConsent.kt",
            "main/java/ma/etherealmber/atlascommerce/core/diagnostics/CrashReportConsent.kt",
            "main/java/ma/etherealmber/atlascommerce/core/diagnostics/DiagnosticReporter.kt",
            "main/java/ma/etherealmber/atlascommerce/core/format/AtlasFormat.kt",
            "main/java/ma/etherealmber/atlascommerce/core/net/ApiProvider.kt",
            "main/java/ma/etherealmber/atlascommerce/core/net/ApiResult.kt",
            "main/java/ma/etherealmber/atlascommerce/core/net/ApiRunner.kt",
            "main/java/ma/etherealmber/atlascommerce/core/net/AtlasApi.kt",
            "main/java/ma/etherealmber/atlascommerce/core/net/DesktopDecisionOneShotInterceptor.kt",
            "main/java/ma/etherealmber/atlascommerce/core/net/DeviceConnectivity.kt",
            "main/java/ma/etherealmber/atlascommerce/core/net/Interceptors.kt",
            "main/java/ma/etherealmber/atlascommerce/core/net/RoleRefreshInterceptor.kt",
            "main/java/ma/etherealmber/atlascommerce/core/net/ServerProbe.kt",
            "main/java/ma/etherealmber/atlascommerce/core/net/ServerUrl.kt",
            "main/java/ma/etherealmber/atlascommerce/core/net/SessionEvents.kt",
            "main/java/ma/etherealmber/atlascommerce/core/net/TokenHolder.kt",
            "main/java/ma/etherealmber/atlascommerce/core/net/VerifyLinkParser.kt",
            "main/java/ma/etherealmber/atlascommerce/core/net/VirtualNetworkGateway.kt",
            "main/java/ma/etherealmber/atlascommerce/core/persist/AtlasPreferences.kt",
            "main/java/ma/etherealmber/atlascommerce/core/security/CryptoStore.kt",
            "main/java/ma/etherealmber/atlascommerce/core/security/DeviceIdentity.kt",
            "main/java/ma/etherealmber/atlascommerce/core/security/DeviceUnlockAuthenticator.kt",
            "main/java/ma/etherealmber/atlascommerce/core/security/DeviceUnlockSetupPrompt.kt",
            "main/java/ma/etherealmber/atlascommerce/core/server/ServerConfig.kt",
            "main/java/ma/etherealmber/atlascommerce/core/session/FreshProcessStartup.kt",
            "main/java/ma/etherealmber/atlascommerce/core/session/SessionManager.kt",
            "main/java/ma/etherealmber/atlascommerce/core/sync/MobilePreferenceBackup.kt",
            "main/java/ma/etherealmber/atlascommerce/core/sync/MobilePreferenceJson.kt",
            "main/java/ma/etherealmber/atlascommerce/core/sync/MobilePreferencesSync.kt",
            "main/java/ma/etherealmber/atlascommerce/core/update/ApkInstallStatusReceiver.kt",
            "main/java/ma/etherealmber/atlascommerce/core/update/AppUpdateManager.kt",
            "main/java/ma/etherealmber/atlascommerce/core/update/UpdateApkTrust.kt",
            "main/java/ma/etherealmber/atlascommerce/core/update/UpdateCheckWorker.kt",
            "main/java/ma/etherealmber/atlascommerce/core/update/UpdateDownloadWorker.kt",
            "main/java/ma/etherealmber/atlascommerce/core/update/UpdateModels.kt",
            "main/java/ma/etherealmber/atlascommerce/core/update/UpdateNotificationActions.kt",
            "main/java/ma/etherealmber/atlascommerce/core/update/UpdateNotifier.kt",
            "main/java/ma/etherealmber/atlascommerce/data/CartStore.kt",
            "main/java/ma/etherealmber/atlascommerce/data/FinanceReportCache.kt",
            "main/java/ma/etherealmber/atlascommerce/data/OfflineCache.kt",
            "main/java/ma/etherealmber/atlascommerce/data/model/Models.kt",
            "main/java/ma/etherealmber/atlascommerce/data/model/ProductUnit.kt",
            "main/java/ma/etherealmber/atlascommerce/data/model/StartupRoleRefresh.kt",
            "main/java/ma/etherealmber/atlascommerce/data/repo/AgentContextNeeds.kt",
            "main/java/ma/etherealmber/atlascommerce/data/repo/AgentPrompt.kt",
            "main/java/ma/etherealmber/atlascommerce/data/repo/AssistSseFrameParser.kt",
            "main/java/ma/etherealmber/atlascommerce/data/repo/AuthRepository.kt",
            "main/java/ma/etherealmber/atlascommerce/data/repo/CommerceRepository.kt",
            "main/java/ma/etherealmber/atlascommerce/data/repo/SnakeScoreRepository.kt",
            "main/java/ma/etherealmber/atlascommerce/di/AppModule.kt",
            "main/java/ma/etherealmber/atlascommerce/di/ProbeClient.kt",
            "main/java/ma/etherealmber/atlascommerce/di/Qualifiers.kt",
            "main/java/ma/etherealmber/atlascommerce/push/AtlasFirebaseMessagingService.kt",
            "main/java/ma/etherealmber/atlascommerce/push/DeviceTokenRequest.kt",
            "main/java/ma/etherealmber/atlascommerce/push/PushTokenRegistrar.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/components/ApiRefreshCooldown.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/components/AppUpdateUi.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/components/AtlasAvatar.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/components/AtlasComponents.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/components/AtlasOnboarding.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/components/AtlasPaginationControls.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/components/AtlasSkeleton.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/components/AtlasSurprises.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/components/BarcodeCameraView.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/components/FinanceReportSelection.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/components/FinanceReportViewers.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/components/FloatingDockInsets.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/components/NativeFinanceReport.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/components/ProductArtwork.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/components/ProductCreateDialog.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/components/ProductImageEncoder.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/components/ProductUnitCatalogViewModel.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/components/StatusUi.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/i18n/AtlasLocalization.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/i18n/NotificationLocalization.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/i18n/ReleaseNotesLocalization.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/i18n/UpdateProgressLocalization.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/AtlasIcons.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/LICENSE-APACHE-2.0.txt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/LICENSE-LUCIDE.txt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/LucideCore.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/NOTICE.md",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/ArrowDownLeft.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/ArrowLeft.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/ArrowLeftRight.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/ArrowRight.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/ArrowUpRight.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/AtSign.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/Bell.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/Bookmark.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/Bot.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/Bug.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/Camera.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/Check.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/ChevronDown.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/ChevronRight.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/ChevronUp.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/CircleAlert.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/CircleCheck.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/CircleDollarSign.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/CircleQuestionMark.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/CloudOff.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/Copy.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/CreditCard.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/Database.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/Download.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/Ellipsis.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/ExternalLink.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/Eye.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/EyeOff.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/FileText.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/FingerprintPattern.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/Gamepad2.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/Globe.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/Grid2x2.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/HandCoins.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/History.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/Hourglass.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/House.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/Images.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/Info.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/Link.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/List.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/LockKeyhole.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/LogOut.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/Mail.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/MessageSquarePlus.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/Minus.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/Monitor.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/MonitorSmartphone.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/Package.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/Palette.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/PenLine.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/Phone.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/Plus.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/QrCode.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/Receipt.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/RefreshCw.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/ScanLine.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/Search.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/Send.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/Server.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/Settings.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/Share2.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/Shield.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/ShieldCheck.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/ShoppingCart.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/SlidersHorizontal.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/Sparkles.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/Square.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/SquareCheck.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/Trash2.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/TrendingUp.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/TriangleAlert.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/UserRound.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/Users.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/Video.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/VideoOff.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/Wallet.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/WandSparkles.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/Wifi.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/X.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/ZoomIn.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/ZoomOut.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/root/AppViewModel.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/root/AtlasRoot.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/agent/AgentAttachmentReader.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/agent/AgentInlineActions.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/agent/AgentLocalActions.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/agent/AgentLocalization.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/agent/AgentScreen.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/agent/AgentViewModel.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/balance/BalanceScreen.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/balance/BalanceViewModel.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/clients/ClientsScreen.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/clients/ClientsViewModel.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/devices/DesktopPhoneApprovalRules.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/devices/QrDeviceScreen.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/devices/QrDeviceViewModel.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/devices/QrTokenParser.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/employees/EmployeeDirectoryScreen.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/employees/EmployeeDirectoryViewModel.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/employees/EmployeesScreen.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/home/HomeScreen.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/home/HomeViewModel.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/legal/LegalScreens.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/login/LoginScreen.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/login/LoginViewModel.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/notifications/NotificationsScreen.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/notifications/NotificationsViewModel.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/products/ProductHistoryDialog.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/products/ProductHistoryViewModel.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/products/ProductStockPresentation.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/products/ProductsScreen.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/products/ProductsViewModel.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/products/StockAdjustmentInput.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/sale/SaleScreen.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/sale/SaleViewModel.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/scan/ScanScreen.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/scan/ScanViewModel.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/server/ServerSetupScreen.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/server/ServerSetupViewModel.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/settings/DataCacheManagerDialog.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/settings/DeveloperDiagnosticsScreen.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/settings/ProfilePresentation.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/settings/SafTextReader.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/settings/SettingsScreen.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/snake/InternetAvailability.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/snake/SnakeGame.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/snake/SnakePersonalBestViewModel.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/snake/SnakeScreen.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/shell/AtlasTapGestures.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/shell/MainShell.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/shell/ShellViewModel.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/shell/SidebarNavigation.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/theme/Theme.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/util/ActivityExt.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/util/Intents.kt",
            "main/res/drawable-nodpi/atlas_wordmark_mask.png",
            "main/res/drawable/ic_launcher_direct.xml",
            "main/res/drawable/ic_launcher_foreground.xml",
            "main/res/drawable/ic_stat_update.xml",
            "main/res/mipmap-anydpi-v26/ic_launcher.xml",
            "main/res/values-ar/strings.xml",
            "main/res/values-en/strings.xml",
            "main/res/values-night/colors.xml",
            "main/res/values-night/themes.xml",
            "main/res/values/colors.xml",
            "main/res/values/strings.xml",
            "main/res/values/themes.xml",
            "main/res/xml/file_paths.xml",
            "test/java/ma/etherealmber/atlascommerce/AgentConversationStateTest.kt",
            "test/java/ma/etherealmber/atlascommerce/AgentLocaleTest.kt",
            "test/java/ma/etherealmber/atlascommerce/AppLanguageTest.kt",
            "test/java/ma/etherealmber/atlascommerce/AppUpdateManagerTest.kt",
            "test/java/ma/etherealmber/atlascommerce/AtlasFormatTest.kt",
            "test/java/ma/etherealmber/atlascommerce/DesktopDecisionOneShotInterceptorTest.kt",
            "test/java/ma/etherealmber/atlascommerce/DeviceUnlockPlatformPolicyTest.kt",
            "test/java/ma/etherealmber/atlascommerce/DiagnosticReportMessageTest.kt",
            "test/java/ma/etherealmber/atlascommerce/FinanceReportCacheTest.kt",
            "test/java/ma/etherealmber/atlascommerce/LocalStorageRecommendationTest.kt",
            "test/java/ma/etherealmber/atlascommerce/LocalizationRegressionTest.kt",
            "test/java/ma/etherealmber/atlascommerce/LoginOtpValidationTest.kt",
            "test/java/ma/etherealmber/atlascommerce/ProfilePresentationTest.kt",
            "test/java/ma/etherealmber/atlascommerce/QrDevicesAndModelsTest.kt",
            "test/java/ma/etherealmber/atlascommerce/SavedAccountDeviceUnlockTest.kt",
            "test/java/ma/etherealmber/atlascommerce/ServerUrlTest.kt",
            "test/java/ma/etherealmber/atlascommerce/SnakeGameTest.kt",
            "test/java/ma/etherealmber/atlascommerce/StockAdjustmentInputTest.kt",
            "test/java/ma/etherealmber/atlascommerce/core/diagnostics/CrashBreadcrumbPersistenceTest.kt",
            "test/java/ma/etherealmber/atlascommerce/core/diagnostics/CrashReportConsentTest.kt",
            "test/java/ma/etherealmber/atlascommerce/core/net/AtlasVersionHeadersTest.kt",
            "test/java/ma/etherealmber/atlascommerce/core/net/RoleRefreshInterceptorTest.kt",
            "test/java/ma/etherealmber/atlascommerce/core/net/SafeReadRetryTest.kt",
            "test/java/ma/etherealmber/atlascommerce/core/net/SessionGuardInvalidationTest.kt",
            "test/java/ma/etherealmber/atlascommerce/core/security/DeviceIdentityTest.kt",
            "test/java/ma/etherealmber/atlascommerce/core/session/FreshProcessStartupTest.kt",
            "test/java/ma/etherealmber/atlascommerce/core/session/ProfileRefreshRoleSafetyTest.kt",
            "test/java/ma/etherealmber/atlascommerce/core/sync/MobilePreferenceBackupTest.kt",
            "test/java/ma/etherealmber/atlascommerce/core/sync/MobilePreferenceFailureParsingTest.kt",
            "test/java/ma/etherealmber/atlascommerce/core/sync/MobilePreferenceSchedulingTest.kt",
            "test/java/ma/etherealmber/atlascommerce/core/update/UpdateApkTrustTest.kt",
            "test/java/ma/etherealmber/atlascommerce/data/model/MobilePreferencesContractTest.kt",
            "test/java/ma/etherealmber/atlascommerce/data/model/ProductRequestWireContractTest.kt",
            "test/java/ma/etherealmber/atlascommerce/data/repo/AgentContextNeedsTest.kt",
            "test/java/ma/etherealmber/atlascommerce/data/repo/AssistSseFrameParserTest.kt",
            "test/java/ma/etherealmber/atlascommerce/push/PushNotificationContentTest.kt",
            "test/java/ma/etherealmber/atlascommerce/ui/icons/AtlasIconsParityTest.kt",
            "test/java/ma/etherealmber/atlascommerce/ui/root/AppResumeRefreshPolicyTest.kt",
            "test/java/ma/etherealmber/atlascommerce/ui/screens/agent/AgentLocalActionsTest.kt",
            "test/java/ma/etherealmber/atlascommerce/ui/screens/agent/AgentStreamingTest.kt",
            "test/java/ma/etherealmber/atlascommerce/ui/screens/products/ProductStockPresentationTest.kt",
            "test/java/ma/etherealmber/atlascommerce/ui/screens/server/ServerSwitchPolicyTest.kt",
            "test/java/ma/etherealmber/atlascommerce/ui/screens/settings/DeveloperDiagnosticsScreenTest.kt",
            "test/java/ma/etherealmber/atlascommerce/ui/theme/AtlasThemePaletteTest.kt",
            "androidTest/java/ma/etherealmber/atlascommerce/ui/components/AtlasMobileWelcomeScreenTest.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/components/AtlasMobileTour.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/components/AtlasMobileWelcome.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/Compass.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/Moon.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/icons/lucide/all/Sun.kt",
            "main/java/ma/etherealmber/atlascommerce/ui/screens/settings/ProfileTimeZone.kt",
            "main/res/drawable-nodpi/welcome_shot_balance.webp",
            "main/res/drawable-nodpi/welcome_shot_home.webp",
            "main/res/drawable-nodpi/welcome_shot_products.webp",
            "main/res/drawable-nodpi/welcome_shot_scan.webp",
            "test/java/ma/etherealmber/atlascommerce/core/session/SessionProfileFieldsTest.kt",
            "test/java/ma/etherealmber/atlascommerce/ui/screens/settings/ProfileTimeZoneTest.kt"
        )));

    private static final String V105_SENTINEL =
        "main/java/ma/etherealmber/atlascommerce/MainActivity.kt";

    private static final String KEEP_MARKER = "@atlas-keep";

    private LegacySourceQuarantine() {}

    private static boolean hasExplicitKeepHeader(Path file) {
        try {
            byte[] all = Files.readAllBytes(file);
            int limit = Math.min(all.length, 512);
            String head = new String(all, 0, limit, StandardCharsets.UTF_8);
            return head.contains(KEEP_MARKER);
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Quarantines stale/untracked files inside {@code srcDir} (such as leftover 1.0.4 files like
     * {@code push/AtlasFirebaseMessagingService.kt}) into {@code quarantineDir} (outside
     * {@code build/}) across both Git checkouts AND GitHub "Download ZIP" extractions without
     * {@code .git}.
     */
    public static List<String> quarantineUntracked(File rootDir, File srcDir, File quarantineDir) {
        if (!srcDir.isDirectory()) {
            return Collections.emptyList();
        }

        Set<String> allowed = computeAllowedRelativePaths(rootDir, srcDir);
        Path srcPath = srcDir.toPath();
        List<Path> candidates = new ArrayList<>();

        try (Stream<Path> walk = Files.walk(srcPath)) {
            walk.filter(Files::isRegularFile).forEach(file -> {
                String rel = srcPath.relativize(file).toString().replace('\\', '/');
                if (!allowed.contains(rel) && !hasExplicitKeepHeader(file)) {
                    candidates.add(file);
                }
            });
        } catch (Exception ignored) {
            return Collections.emptyList();
        }

        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> moved = new ArrayList<>();
        for (Path candidate : candidates) {
            String relPath = srcPath.relativize(candidate).toString().replace('\\', '/');
            Path target = quarantineDir.toPath().resolve(relPath);
            try {
                Files.createDirectories(target.getParent());
                Files.copy(candidate, target, StandardCopyOption.REPLACE_EXISTING);
                removeOrNeutralizeOnWindowsLock(candidate);
                moved.add(relPath);
            } catch (Exception ignored) {
                neutralizeSourceInPlace(candidate);
            }
        }

        if (!moved.isEmpty()) {
            pruneEmptyDirectories(srcPath);
        }
        return moved;
    }

    private static void removeOrNeutralizeOnWindowsLock(Path candidate) {
        candidate.toFile().setWritable(true);
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                Files.deleteIfExists(candidate);
                return;
            } catch (Exception e) {
                if (attempt < 4) {
                    try {
                        Thread.sleep(40L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        neutralizeSourceInPlace(candidate);
    }

    private static void neutralizeSourceInPlace(Path candidate) {
        String name = candidate.getFileName().toString();
        if (name.endsWith(".kt") || name.endsWith(".java")) {
            try {
                candidate.toFile().setWritable(true);
                Files.writeString(
                    candidate,
                    "// Quarantined legacy 1.0.4 file (backup in .atlas-1.0.4-quarantine)\n",
                    StandardCharsets.UTF_8
                );
            } catch (Exception ignored) {
            }
        }
    }

    private static Set<String> computeAllowedRelativePaths(File rootDir, File srcDir) {
        Set<String> allowed = new HashSet<>(EXPECTED_APP_SRC_FILES);
        if (new File(rootDir, ".git").exists()) {
            String gitBin = findGitExecutable();
            if (gitBin != null) {
                Set<String> tracked = queryGitTrackedFiles(gitBin, rootDir, srcDir);
                if (tracked.contains(V105_SENTINEL)) {
                    allowed.addAll(tracked);
                }
            }
        }
        return allowed;
    }

    private static Set<String> queryGitTrackedFiles(String gitBin, File rootDir, File srcDir) {
        try {
            String srcRel = rootDir.toPath().relativize(srcDir.toPath()).toString().replace('\\', '/');
            Process proc = new ProcessBuilder(
                gitBin, "-c", "safe.directory=*", "ls-files", "-z", "--", srcRel
            )
                .directory(rootDir)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
            byte[] stdout = proc.getInputStream().readAllBytes();
            if (proc.waitFor() != 0) {
                return Collections.emptySet();
            }
            String decoded = new String(stdout, StandardCharsets.UTF_8);
            if (decoded.isEmpty()) {
                return Collections.emptySet();
            }
            String prefix = srcRel + "/";
            Set<String> tracked = new HashSet<>();
            for (String entry : decoded.split("\0")) {
                String normalized = entry.replace('\\', '/');
                if (normalized.startsWith(prefix)) {
                    tracked.add(normalized.substring(prefix.length()));
                }
            }
            return tracked;
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    private static void pruneEmptyDirectories(Path srcRoot) {
        try (Stream<Path> walk = Files.walk(srcRoot)) {
            walk.sorted(Comparator.reverseOrder())
                .filter(p -> !p.equals(srcRoot) && Files.isDirectory(p))
                .forEach(dir -> {
                    try (Stream<Path> entries = Files.list(dir)) {
                        if (entries.findAny().isEmpty()) {
                            Files.deleteIfExists(dir);
                        }
                    } catch (Exception ignored) {
                    }
                });
        } catch (Exception ignored) {
        }
    }

    static String findGitExecutable() {
        // Never ask ProcessBuilder to resolve a bare "git" command through PATH: build
        // environments may inherit a PATH containing attacker-controlled directories.
        List<String> candidates = new ArrayList<>(List.of(
            "/usr/bin/git",
            "/usr/local/bin/git",
            "/opt/homebrew/bin/git",
            "/opt/local/bin/git",
            "C:/Program Files/Git/cmd/git.exe",
            "C:/Program Files (x86)/Git/cmd/git.exe"
        ));
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank() && new File(localAppData).isAbsolute()) {
            candidates.add(new File(localAppData, "Programs/Git/cmd/git.exe").getPath());
            File ghDesktop = new File(localAppData, "GitHubDesktop");
            File[] children = ghDesktop.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (child.isDirectory() && child.getName().startsWith("app-")) {
                        candidates.add(new File(child, "resources/app/git/cmd/git.exe").getPath());
                    }
                }
            }
        }
        for (String candidate : candidates) {
            File executable = new File(candidate);
            if (!executable.isAbsolute() || !executable.isFile() || !executable.canExecute()) {
                continue;
            }
            try {
                Process p = new ProcessBuilder(executable.getAbsolutePath(), "--version")
                    .redirectErrorStream(true)
                    .start();
                p.getInputStream().readAllBytes();
                if (p.waitFor() == 0) {
                    return executable.getAbsolutePath();
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
