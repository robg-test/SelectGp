
import com.intellij.openapi.components.*

@Service(Service.Level.APP)
@State(
    name = "SelectGpSettings",
    storages = [Storage("myPluginSettings.xml")]
)
class SelectGpSettings : PersistentStateComponent<SelectGpSettings.State> {

    data class State(
        var provider: String = "OpenAI ChatGpt",
        var enabled: Boolean = true
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    companion object {
        fun getInstance(): SelectGpSettings =
            service()
    }
}