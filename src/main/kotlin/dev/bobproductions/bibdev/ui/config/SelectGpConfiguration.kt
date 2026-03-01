package dev.bobproductions.bibdev.ui.config
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.options.Configurable
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import dev.bobproductions.bibdev.service.api.GptModelRequestInvoker
import dev.bobproductions.bibdev.service.builders.GptModelRequestBuilder
import loadGptApiKey
import saveGptApiKey
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Desktop
import java.awt.event.MouseAdapter
import javax.swing.*

class SelectGpConfiguration(
    private val connectionTester: (String) -> Boolean = { apiKey ->
        val requestBuilder = GptModelRequestBuilder()
        requestBuilder.apiKey = apiKey
        val invoker = GptModelRequestInvoker(requestBuilder.buildTestRequest())
        invoker.canCommunicateWithModel()
        true
    },
    private val apiKeyLoader: () -> String? = { loadGptApiKey()?.toString() },
    private val apiKeySaver: (String) -> Unit = { key -> saveGptApiKey(key) }
) : Configurable {

    private lateinit var providerField: JTextField
    private lateinit var enabledCheckBox: JCheckBox
    private lateinit var apiKeyButton: JButton
    private lateinit var testConnectionButton: JButton
    private lateinit var gptLabel: JLabel
    private lateinit var apiKeyField: JBPasswordField

    override fun createComponent(): JComponent {
        providerField = JBTextField()
        apiKeyField = JBPasswordField().apply {
            maximumSize = preferredSize
        }
        enabledCheckBox = JBCheckBox("Enable plugin")
        apiKeyButton = JButton("API Key")
        testConnectionButton = JButton("Test").apply {
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: java.awt.event.MouseEvent?) {
                    val apiKey = String(apiKeyField.password)
                    try {
                        connectionTester(apiKey)
                        ApplicationManager.getApplication().invokeLater {
                            Messages.showDialog(
                                "Successfully connected to OpenAI",
                                "SuperGrunk Ready",
                                arrayOf("OK"),
                                0,
                                AllIcons.General.InspectionsOK
                            )
                        }
                    } catch (e: Exception) {
                        Messages.showErrorDialog(
                            "Failed to connect to OpenAI: ${e.message}",
                            "SuperGrunk Failure"
                        )
                    }
                }
            })
        }
        gptLabel = JLabel("<html><a href='https://platform.openai.com/api-keys'>Get a OpenAI API Key</a></html>")
            .apply {
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: java.awt.event.MouseEvent?) {
                        Desktop.getDesktop().browse(java.net.URI("https://platform.openai.com/api-keys"))
                    }
                })
            }

        val fixedSize = apiKeyField.preferredSize
        apiKeyField.minimumSize = fixedSize
        apiKeyField.preferredSize = fixedSize
        apiKeyField.maximumSize = fixedSize

        val apiKeyPanel = JPanel(BorderLayout(8, 0)).apply {
            add(apiKeyField, BorderLayout.CENTER)
            add(testConnectionButton, BorderLayout.EAST)
        }

        val form = FormBuilder.createFormBuilder()
            .addLabeledComponent("Provider", JComboBox(arrayOf("OpenAI ChatGpt")).apply {
                selectedItem = SelectGpSettings.getInstance().state.provider
            })
            .addLabeledComponent("Set API Key", apiKeyPanel)
            .addLabeledComponent("", gptLabel)
            .addComponent(enabledCheckBox)
            .panel

        return JPanel(BorderLayout()).apply {
            add(form, BorderLayout.NORTH)
        }
    }

    override fun isModified(): Boolean {
        val settings = SelectGpSettings.getInstance().state
        return providerField.text != settings.provider ||
            enabledCheckBox.isSelected != settings.enabled
    }

    override fun apply() {
        val settings = SelectGpSettings.getInstance().state
        settings.provider = providerField.text
        settings.enabled = enabledCheckBox.isSelected
        apiKeySaver(String(apiKeyField.password))
    }

    override fun reset() {
        val settings = SelectGpSettings.getInstance().state
        providerField.text = settings.provider
        enabledCheckBox.isSelected = settings.enabled
        ApplicationManager.getApplication().executeOnPooledThread {
            val apiKey = apiKeyLoader()
            ApplicationManager.getApplication().invokeLater {
                apiKeyField.text = apiKey.orEmpty()
            }
        }
    }

    override fun getDisplayName(): String = "SuperGrunk Plugin"
}