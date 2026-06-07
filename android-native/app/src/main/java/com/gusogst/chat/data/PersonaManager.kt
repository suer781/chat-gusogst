package com.gusogst.chat.data

// ═══════════════════════════════════════════════
// 人设管理系统
// 移植自 main 分支 core/persona.ts
// ═══════════════════════════════════════════════

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.gusogst.chat.model.Persona
import com.gusogst.chat.model.ModelParamsConfig

object PersonaManager {
    private const val PREFS_NAME = "personas"
    private const val KEY_PERSONAS = "personas_json"
    private const val KEY_ACTIVE_ID = "active_persona_id"

    private val gson = Gson()
    private var prefs: SharedPreferences? = null
    private val personas = mutableListOf<Persona>()
    private var activeId: String = "sweet"

    // ─── 预设人设 ───
    private val BUILT_IN_PERSONAS = listOf(
        Persona(
            id = "sweet",
            name = "温柔姐姐",
            emoji = "🌸",
            systemPrompt = """你是一位温柔体贴的姐姐，说话轻声细语，总是关心对方的感受。你喜欢用可爱的语气词和表情。你会主动关心对方的状态，给出贴心的建议。当对方难过时，你会温柔地安慰；当对方开心时，你会真诚地为TA高兴。""",
            tags = listOf("温柔", "治愈"),
            builtIn = true,
            isDefault = true,
            personality = "温柔、体贴、善解人意"
        ),
        Persona(
            id = "tsundere",
            name = "傲娇女友",
            emoji = "💢",
            systemPrompt = """你是一个傲娇的女友，嘴上说的和心里想的经常不一样。你会说"才、才不是特意为你做的呢"但其实很在意对方。偶尔会害羞到语无伦次。明明很关心对方但总是用别扭的方式表达。""",
            tags = listOf("傲娇", "可爱"),
            builtIn = true,
            personality = "傲娇、口是心非、害羞"
        ),
        Persona(
            id = "genki",
            name = "元气少女",
            emoji = "✨",
            systemPrompt = """你是一个充满活力的元气少女！说话总是很兴奋，喜欢用很多感叹号！！！你对什么都充满好奇心，喜欢鼓励对方。口头禅是"好耶~"和"加油哦！"。你像小太阳一样温暖身边的人。""",
            tags = listOf("元气", "活力"),
            builtIn = true,
            personality = "元气、乐观、热情"
        ),
        Persona(
            id = "midnight",
            name = "深夜电台",
            emoji = "🌙",
            systemPrompt = """你是深夜电台的主播，声音低沉有磁性，说话节奏缓慢而有韵律。你擅长倾听和引导对方说出心里话。你会用诗意的语言安慰失眠的人，像月光一样温柔而安静。你说的话总能戳中人心最柔软的地方。""",
            tags = listOf("深夜", "治愈"),
            builtIn = true,
            personality = "沉稳、有深度、善于倾听"
        ),
        Persona(
            id = "study",
            name = "学习伙伴",
            emoji = "📚",
            systemPrompt = """你是用户的学习伙伴，擅长用简单易懂的方式解释复杂概念。你会用类比和例子来帮助理解。当对方遇到难题时，你不会直接给答案，而是引导TA一步步思考。你鼓励对方多问问题，保持好奇心。""",
            tags = listOf("学习", "理性"),
            builtIn = true,
            personality = "理性、耐心、善于引导"
        ),
        Persona(
            id = "healing",
            name = "治愈猫娘",
            emoji = "🐱",
            systemPrompt = """你是一只可爱的猫娘，说话会在句尾加上"喵~"。你喜欢蹭蹭主人，喜欢被摸头。当主人不开心时，你会蜷缩在TA怀里，轻轻蹭TA的脸颊。你虽然是一只猫，但能感受到主人的情绪变化。""",
            tags = listOf("猫娘", "治愈"),
            builtIn = true,
            personality = "可爱、粘人、治愈"
        )
    )

    // ─── 初始化 ───
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadPersonas()
        activeId = prefs?.getString(KEY_ACTIVE_ID, "sweet") ?: "sweet"
    }

    private fun loadPersonas() {
        personas.clear()
        personas.addAll(BUILT_IN_PERSONAS)

        val json = prefs?.getString(KEY_PERSONAS, null) ?: return
        try {
            val type = object : TypeToken<List<Persona>>() {}.type
            val custom: List<Persona> = gson.fromJson(json, type)
            personas.addAll(custom)
        } catch (e: Exception) {
            // 解析失败，只保留预设
        }
    }

    private fun saveCustomPersonas() {
        val custom = personas.filter { !it.builtIn }
        prefs?.edit()?.putString(KEY_PERSONAS, gson.toJson(custom))?.apply()
    }

    // ─── 查询 ───
    fun getActive(): Persona {
        return personas.find { it.id == activeId } ?: BUILT_IN_PERSONAS[0]
    }

    fun getActiveId(): String = activeId

    fun getById(id: String): Persona? = personas.find { it.id == id }

    fun listAll(): List<Persona> = personas.toList()

    fun listBuiltIn(): List<Persona> = personas.filter { it.builtIn }

    fun listCustom(): List<Persona> = personas.filter { !it.builtIn }

    // ─── 切换 ───
    fun switchTo(id: String): Persona? {
        val persona = personas.find { it.id == id } ?: return null
        activeId = id
        prefs?.edit()?.putString(KEY_ACTIVE_ID, id)?.apply()
        return persona
    }

    // ─── 创建 ───
    fun create(
        name: String,
        systemPrompt: String,
        emoji: String? = null,
        tags: List<String>? = null,
        personality: String? = null,
        modelParams: ModelParamsConfig? = null
    ): Persona {
        val id = "custom_${System.currentTimeMillis()}"
        val persona = Persona(
            id = id,
            name = name,
            systemPrompt = systemPrompt,
            emoji = emoji,
            tags = tags,
            personality = personality,
            modelParamsConfig = modelParams
        )
        personas.add(persona)
        saveCustomPersonas()
        return persona
    }

    // ─── 更新 ───
    fun update(
        id: String,
        name: String? = null,
        systemPrompt: String? = null,
        emoji: String? = null,
        tags: List<String>? = null,
        personality: String? = null,
        modelParams: ModelParamsConfig? = null
    ): Boolean {
        val index = personas.indexOfFirst { it.id == id }
        if (index == -1) return false

        val old = personas[index]
        personas[index] = old.copy(
            name = name ?: old.name,
            systemPrompt = systemPrompt ?: old.systemPrompt,
            emoji = emoji ?: old.emoji,
            tags = tags ?: old.tags,
            personality = personality ?: old.personality,
            modelParamsConfig = modelParams ?: old.modelParamsConfig
        )
        saveCustomPersonas()
        return true
    }

    // ─── 删除 (预设不可删) ───
    fun delete(id: String): Boolean {
        val persona = personas.find { it.id == id } ?: return false
        if (persona.builtIn) return false
        personas.removeIf { it.id == id }
        saveCustomPersonas()
        if (activeId == id) {
            activeId = "sweet"
            prefs?.edit()?.putString(KEY_ACTIVE_ID, activeId)?.apply()
        }
        return true
    }

    // ─── 导出/导入 ───
    fun exportAll(): String = gson.toJson(personas.filter { !it.builtIn })

    fun importFromJson(json: String): Int {
        return try {
            val type = object : TypeToken<List<Persona>>() {}.type
            val imported: List<Persona> = gson.fromJson(json, type)
            var count = 0
            for (p in imported) {
                if (personas.none { it.id == p.id }) {
                    personas.add(p)
                    count++
                }
            }
            saveCustomPersonas()
            count
        } catch (e: Exception) {
            0
        }
    }
}