/**
 * Mixify Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.mixify.music.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.mixify.innertube.YouTube
import com.mixify.innertube.models.YouTubeAccount
import com.mixify.innertube.utils.parseCookieString
import com.mixify.music.LocalPlayerAwareWindowInsets
import com.mixify.music.R
import com.mixify.music.constants.AccountChannelHandleKey
import com.mixify.music.constants.AccountEmailKey
import com.mixify.music.constants.AccountNameKey
import com.mixify.music.constants.DataSyncIdKey
import com.mixify.music.constants.InnerTubeAuthUserKey
import com.mixify.music.constants.InnerTubeCookieKey
import com.mixify.music.constants.VisitorDataKey
import com.mixify.music.ui.component.IconButton
import com.mixify.music.ui.utils.backToMain
import com.mixify.music.utils.reportException
import com.mixify.music.utils.safeDataStoreEdit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

private enum class LoginStage {
    Authenticating,
    CheckingAccounts,
    SelectingAccount,
    SwitchingAccount,
    Completing,
}

private data class AuthData(
    val cookie: String,
    val visitorData: String,
    val dataSyncId: String,
    val authUser: String,
)

@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    isSwitchingChannel: Boolean = false,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val jsInterface = remember { LoginJsInterface() }
    var loginStage by remember { mutableStateOf(LoginStage.Authenticating) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var accounts by remember { mutableStateOf<List<YouTubeAccount>>(emptyList()) }
    var selectedAccount by remember { mutableStateOf<YouTubeAccount?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedGoogleEmail by remember { mutableStateOf<String?>(null) }
    var isGoogleSignInInProgress by remember { mutableStateOf(false) }

    val googleWebClientId = stringResource(R.string.google_web_client_id)
    val noGoogleAccountsMsg = stringResource(R.string.no_google_accounts_found)
    val googleSignInFailedMsg = stringResource(R.string.google_sign_in_failed)
    val googleAccountMismatchFormat = stringResource(R.string.google_account_mismatch)

    fun launchGoogleSignIn() {
        isGoogleSignInInProgress = true
        coroutineScope.launch {
            val credentialManager = CredentialManager.create(context)

            // Preferred flow per requirement 4:
            // 1. Try authorized accounts first: setFilterByAuthorizedAccounts(true), setAutoSelectEnabled(true)
            val authorizedOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(googleWebClientId)
                .setAutoSelectEnabled(true)
                .build()

            val authorizedRequest = GetCredentialRequest.Builder()
                .addCredentialOption(authorizedOption)
                .build()

            var credentialResult = runCatching {
                credentialManager.getCredential(context, authorizedRequest)
            }.getOrNull()

            // 2. If no authorized credentials are available, retry with setFilterByAuthorizedAccounts(false)
            if (credentialResult == null) {
                val allAccountsOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(googleWebClientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val allAccountsRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(allAccountsOption)
                    .build()

                try {
                    credentialResult = credentialManager.getCredential(context, allAccountsRequest)
                } catch (_: GetCredentialCancellationException) {
                    Timber.d("Google Sign-In cancelled by user")
                    isGoogleSignInInProgress = false
                    return@launch
                } catch (e: NoCredentialException) {
                    logGoogleSignInDiagnostic(context, e, googleWebClientId)
                    errorMessage = noGoogleAccountsMsg
                    isGoogleSignInInProgress = false
                    return@launch
                } catch (e: GetCredentialException) {
                    logGoogleSignInDiagnostic(context, e, googleWebClientId)
                    errorMessage = String.format(googleSignInFailedMsg, e.message ?: e.toString())
                    isGoogleSignInInProgress = false
                    return@launch
                } catch (e: Exception) {
                    logGoogleSignInDiagnostic(context, e, googleWebClientId)
                    errorMessage = String.format(googleSignInFailedMsg, e.message ?: e.toString())
                    isGoogleSignInInProgress = false
                    return@launch
                }
            }

            val credential = credentialResult.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                try {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val email = googleIdTokenCredential.id
                    selectedGoogleEmail = email
                    Timber.i("Google Sign-In successful for Google account: $email")

                    // Direct Google login in WebView with Email pre-filled for selected account
                    val redirectUrl = "https://accounts.google.com/ServiceLogin?Email=${Uri.encode(email)}&continue=https%3A%2F%2Fmusic.youtube.com"
                    webViewRef?.loadUrl(redirectUrl)
                } catch (e: GoogleIdTokenParsingException) {
                    Timber.e(e, "Failed to parse Google ID Token credential")
                    errorMessage = String.format(googleSignInFailedMsg, e.message ?: e.toString())
                }
            } else {
                Timber.e("Unexpected credential type: ${credential.type}")
                errorMessage = String.format(googleSignInFailedMsg, "Unexpected credential type")
            }
            isGoogleSignInInProgress = false
        }
    }

    suspend fun extractAuthData(webView: WebView?): AuthData? {
        val view = webView ?: return null
        var hasAuthCookie = false
        var hasVisitorData = false
        repeat(20) {
            val cookie = CookieManager.getInstance().getCookie("https://music.youtube.com").orEmpty()
            val cookieMap = runCatching { parseCookieString(cookie) }.getOrDefault(emptyMap())
            val visitorDataDeferred = CompletableDeferred<String?>()
            val dataSyncIdDeferred = CompletableDeferred<String?>()
            val authUserDeferred = CompletableDeferred<String?>()
            jsInterface.onVisitorDataReceived = { visitorData ->
                if (!visitorDataDeferred.isCompleted) visitorDataDeferred.complete(visitorData)
            }
            jsInterface.onDataSyncIdReceived = { dataSyncId ->
                if (!dataSyncIdDeferred.isCompleted) dataSyncIdDeferred.complete(dataSyncId)
            }
            jsInterface.onAuthUserReceived = { authUser ->
                if (!authUserDeferred.isCompleted) authUserDeferred.complete(authUser)
            }

            view.loadUrl(
                "javascript:Android.onRetrieveVisitorData(" +
                    "window.yt&&window.yt.config_?window.yt.config_.VISITOR_DATA:null)",
            )
            view.loadUrl(
                "javascript:Android.onRetrieveDataSyncId(" +
                    "window.yt&&window.yt.config_?window.yt.config_.DATASYNC_ID:null)",
            )
            view.loadUrl(
                "javascript:Android.onRetrieveAuthUser(" +
                    "window.yt&&window.yt.config_?String(window.yt.config_.SESSION_INDEX||0):'0')",
            )

            val pageAuthData =
                withTimeoutOrNull(1_000) {
                    WebAuthData(
                        visitorData = visitorDataDeferred.await(),
                        dataSyncId = dataSyncIdDeferred.await(),
                        authUser = authUserDeferred.await().orEmpty(),
                    )
                }
            hasAuthCookie = "SAPISID" in cookieMap
            val visitorData = pageAuthData?.visitorData
            hasVisitorData = !visitorData.isNullOrBlank()
            if (hasAuthCookie && pageAuthData != null && !visitorData.isNullOrBlank()) {
                return AuthData(
                    cookie = cookie,
                    visitorData = visitorData,
                    dataSyncId = pageAuthData.dataSyncId.orEmpty().substringBefore("||"),
                    authUser = pageAuthData.authUser.filter(Char::isDigit).ifBlank { "0" },
                )
            }
            delay(500)
        }

        Timber.w(
            "Login: Timed out waiting for the YouTube Music session " +
                "(authCookie=$hasAuthCookie, visitorData=$hasVisitorData)",
        )
        return null
    }

    suspend fun finalizeLogin(authData: AuthData) {
        loginStage = LoginStage.Completing
        YouTube.cookie = authData.cookie
        YouTube.visitorData = authData.visitorData
        YouTube.dataSyncId = authData.dataSyncId
        YouTube.authUser = authData.authUser

        try {
            val accountInfo = YouTube.accountInfo().getOrThrow()

            // Verify logged-in account corresponds to selected Google account if chosen via account chooser
            val expectedEmail = selectedGoogleEmail
            if (!expectedEmail.isNullOrBlank() && !accountInfo.email.isNullOrBlank()) {
                if (!accountInfo.email.equals(expectedEmail, ignoreCase = true)) {
                    Timber.e("Account mismatch: selected $expectedEmail but YouTube account email is ${accountInfo.email}")
                    errorMessage = String.format(googleAccountMismatchFormat, accountInfo.email, expectedEmail)
                    loginStage = LoginStage.Authenticating
                    return
                }
            }

            val saved =
                withContext(Dispatchers.IO) {
                    context.safeDataStoreEdit { settings ->
                        settings[InnerTubeCookieKey] = authData.cookie
                        settings[VisitorDataKey] = authData.visitorData
                        settings[DataSyncIdKey] = authData.dataSyncId
                        settings[InnerTubeAuthUserKey] = authData.authUser
                        settings[AccountNameKey] = accountInfo.name
                        settings[AccountEmailKey] = accountInfo.email.orEmpty()
                        settings[AccountChannelHandleKey] = accountInfo.channelHandle.orEmpty()
                    }
                }
            check(saved) { "Failed to persist account data" }

            webViewRef?.apply {
                stopLoading()
                clearHistory()
                clearCache(true)
                clearFormData()
            }
            CookieManager.getInstance().flush()

            withContext(Dispatchers.Main) {
                context.packageManager
                    .getLaunchIntentForPackage(context.packageName)
                    ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) }
                    ?.let(context::startActivity)
                Runtime.getRuntime().exit(0)
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (throwable: Exception) {
            Timber.e(throwable, "Login: Failed to validate or save the selected YouTube channel")
            reportException(throwable)
            errorMessage =
                context.getString(
                    R.string.login_failed_with_error,
                    throwable.message ?: context.getString(R.string.error_unknown),
                )
            loginStage = LoginStage.Authenticating
        }
    }

    suspend fun checkAvailableAccounts(authData: AuthData) {
        loginStage = LoginStage.CheckingAccounts
        YouTube.cookie = authData.cookie
        YouTube.visitorData = authData.visitorData
        YouTube.dataSyncId = authData.dataSyncId
        YouTube.authUser = authData.authUser

        val availableAccounts =
            YouTube.accountsList().getOrElse { throwable ->
                Timber.e(throwable, "Login: Failed to retrieve YouTube channels")
                if (isSwitchingChannel) {
                    errorMessage = context.getString(R.string.youtube_channels_failed)
                    loginStage = LoginStage.Authenticating
                    return
                }
                emptyList()
            }

        if (isSwitchingChannel && availableAccounts.isEmpty()) {
            errorMessage = context.getString(R.string.youtube_channels_failed)
            loginStage = LoginStage.Authenticating
        } else if (availableAccounts.size > 1) {
            val currentAccount =
                availableAccounts.firstOrNull { account ->
                    account.pageId == authData.dataSyncId ||
                        account.dataSyncId?.substringBefore("||") == authData.dataSyncId
                } ?: availableAccounts.firstOrNull { it.isSelected }
            accounts = availableAccounts.map { it.copy(isSelected = it == currentAccount) }
            selectedAccount = accounts.firstOrNull { it.isSelected } ?: accounts.first()
            loginStage = LoginStage.SelectingAccount
        } else {
            finalizeLogin(authData)
        }
    }

    fun handleAuthenticatedPage() {
        when (loginStage) {
            LoginStage.Authenticating,
            LoginStage.SwitchingAccount,
            -> {
                val stage = loginStage
                loginStage = LoginStage.CheckingAccounts
                coroutineScope.launch {
                    val authData =
                        extractAuthData(webViewRef) ?: run {
                            errorMessage = context.getString(R.string.authentication_data_extraction_failed)
                            loginStage = stage
                            return@launch
                        }
                    if (stage == LoginStage.SwitchingAccount) {
                        val selectedDataSyncId = selectedAccount?.dataSyncId?.substringBefore("||")
                        finalizeLogin(
                            authData.copy(
                                dataSyncId = selectedDataSyncId?.takeIf(String::isNotBlank) ?: authData.dataSyncId,
                            ),
                        )
                    } else {
                        checkAvailableAccounts(authData)
                    }
                }
            }

            else -> Unit
        }
    }

    Column(
        modifier =
            Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                .fillMaxSize(),
    ) {
        TopAppBar(
            title = {
                Text(
                    stringResource(
                        if (isSwitchingChannel) R.string.switch_youtube_channel else R.string.login,
                    ),
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = navController::navigateUp,
                    onLongClick = navController::backToMain,
                ) {
                    Icon(
                        painterResource(R.drawable.arrow_back),
                        contentDescription = null,
                    )
                }
            },
        )

        if (!isSwitchingChannel) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(
                    onClick = { launchGoogleSignIn() },
                    enabled = !isGoogleSignInInProgress && loginStage == LoginStage.Authenticating,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.account),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.continue_with_google),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.or_sign_in_via_web),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }
            }
        }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { webViewContext ->
                    WebView(webViewContext).apply {
                        webViewClient =
                            object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView,
                                    request: WebResourceRequest,
                                ): Boolean {
                                    val scheme = request.url.scheme
                                    if (scheme == "http" || scheme == "https") return false

                                    if (scheme == "intent") {
                                        runCatching {
                                            Intent.parseUri(request.url.toString(), Intent.URI_INTENT_SCHEME)
                                                .apply {
                                                    addCategory(Intent.CATEGORY_BROWSABLE)
                                                    component = null
                                                    selector = null
                                                }.let(context::startActivity)
                                        }
                                    }
                                    return true
                                }

                                override fun onPageFinished(
                                    view: WebView,
                                    url: String?,
                                ) {
                                    val pageUri = url?.let(Uri::parse)
                                    if (pageUri?.scheme == "https" && pageUri.host == "music.youtube.com") {
                                        handleAuthenticatedPage()
                                    }
                                }
                            }
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                        }
                        addJavascriptInterface(jsInterface, "Android")
                        webViewRef = this
                        if (isSwitchingChannel) {
                            restoreYouTubeCookies(YouTube.cookie)
                        }
                        loadUrl(
                            if (isSwitchingChannel) {
                                "https://music.youtube.com"
                            } else {
                                "https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmusic.youtube.com"
                            },
                        )
                    }
                },
            )

            if (loginStage == LoginStage.CheckingAccounts || loginStage == LoginStage.Completing || isGoogleSignInInProgress) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }

    if (loginStage == LoginStage.SelectingAccount) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.choose_youtube_channel)) },
            text = {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp),
                ) {
                    Text(
                        text = stringResource(R.string.choose_youtube_channel_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(16.dp))
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                    ) {
                        accounts.forEach { account ->
                            YouTubeAccountPickerItem(
                                account = account,
                                isSelected = account == selectedAccount,
                                onClick = { selectedAccount = account },
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = selectedAccount?.let { it.isSelected || it.signinUrl != null } == true,
                    onClick = {
                        val account = selectedAccount ?: return@Button
                        if (account.isSelected) {
                            loginStage = LoginStage.Completing
                            coroutineScope.launch {
                                val authData =
                                    extractAuthData(webViewRef) ?: run {
                                        errorMessage = context.getString(R.string.authentication_data_extraction_failed)
                                        loginStage = LoginStage.Authenticating
                                        return@launch
                                    }
                                finalizeLogin(authData)
                            }
                        } else {
                            val signinUrl = account.signinUrl ?: return@Button
                            val absoluteUrl =
                                when {
                                    signinUrl.startsWith("http://") || signinUrl.startsWith("https://") -> signinUrl
                                    signinUrl.startsWith("/") -> "https://music.youtube.com$signinUrl"
                                    else -> "https://music.youtube.com/$signinUrl"
                                }
                            loginStage = LoginStage.SwitchingAccount
                            webViewRef?.loadUrl(absoluteUrl)
                        }
                    },
                ) {
                    Text(stringResource(R.string.continue_action))
                }
            },
        )
    }

    errorMessage?.let { error ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text(stringResource(R.string.login_failed)) },
            text = { Text(error) },
            confirmButton = {
                Button(
                    onClick = {
                        errorMessage = null
                        webViewRef?.reload()
                    },
                ) {
                    Text(stringResource(R.string.retry))
                }
            },
        )
    }

    BackHandler(
        enabled = webViewRef?.canGoBack() == true && loginStage == LoginStage.Authenticating && errorMessage == null,
    ) {
        webViewRef?.goBack()
    }
}

@Composable
private fun YouTubeAccountPickerItem(
    account: YouTubeAccount,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (account.thumbnailUrl != null) {
                AsyncImage(
                    model = account.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.account),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                account.channelHandle?.let { channelHandle ->
                    Text(
                        text = channelHandle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                account.byline?.let { byline ->
                    Text(
                        text = byline,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            RadioButton(selected = isSelected, onClick = null)
        }
    }
}

private class LoginJsInterface {
    var onVisitorDataReceived: ((String?) -> Unit)? = null
    var onDataSyncIdReceived: ((String?) -> Unit)? = null
    var onAuthUserReceived: ((String?) -> Unit)? = null

    @JavascriptInterface
    fun onRetrieveVisitorData(visitorData: String?) {
        onVisitorDataReceived?.invoke(visitorData)
    }

    @JavascriptInterface
    fun onRetrieveDataSyncId(dataSyncId: String?) {
        onDataSyncIdReceived?.invoke(dataSyncId)
    }

    @JavascriptInterface
    fun onRetrieveAuthUser(authUser: String?) {
        onAuthUserReceived?.invoke(authUser)
    }
}

private data class WebAuthData(
    val visitorData: String?,
    val dataSyncId: String?,
    val authUser: String,
)

private fun isWebClientIdConfigured(clientId: String): Boolean {
    if (clientId.isBlank()) return false
    if (clientId.contains("dummy", ignoreCase = true)) return false
    if (clientId.contains("1000000000000")) return false
    return clientId.endsWith(".apps.googleusercontent.com", ignoreCase = true)
}

private fun logGoogleSignInDiagnostic(context: Context, exception: Exception, webClientId: String) {
    val isConfigured = isWebClientIdConfigured(webClientId)
    val playServicesCode = runCatching {
        val clazz = Class.forName("com.google.android.gms.common.GoogleApiAvailability")
        val getInstance = clazz.getMethod("getInstance")
        val instance = getInstance.invoke(null)
        val method = clazz.getMethod("isGooglePlayServicesAvailable", Context::class.java)
        method.invoke(instance, context) as Int
    }.getOrDefault(-1)

    Timber.w(
        "Google Sign-In Diagnostic:\n" +
            "- Exception Type: %s\n" +
            "- Exception Message: %s\n" +
            "- Package Name: %s\n" +
            "- Play Services Code: %d\n" +
            "- Web Client ID Configured/Non-Placeholder: %b\n" +
            "- Web Client ID Length: %d",
        exception.javaClass.name,
        exception.message,
        context.packageName,
        playServicesCode,
        isConfigured,
        webClientId.length,
    )
}

private fun restoreYouTubeCookies(cookie: String?) {
    val cookieManager = CookieManager.getInstance()
    cookieManager.setAcceptCookie(true)
    cookie
        ?.let(::parseCookieString)
        .orEmpty()
        .forEach { (name, value) ->
            cookieManager.setCookie(
                "https://music.youtube.com",
                "$name=$value; Domain=.youtube.com; Path=/; Secure",
            )
        }
    cookieManager.flush()
}
