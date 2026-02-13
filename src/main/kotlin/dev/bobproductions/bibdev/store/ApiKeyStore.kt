import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.OneTimeString

fun saveGptApiKey(apiKey: String) {
    val attributes = CredentialAttributes(
        "SelectGp.GptApiKey"
    )

    PasswordSafe.instance.set(
        attributes,
        Credentials("gpt-api-key", apiKey)
    )
}

fun loadGptApiKey(): OneTimeString? {
    val attributes = CredentialAttributes("SelectGp.GptApiKey")
    return PasswordSafe.instance.get(attributes)?.password
}
