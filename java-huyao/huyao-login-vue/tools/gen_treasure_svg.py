# -*- coding: utf-8 -*-
"""为狐缘阁「法宝图鉴」的每件法宝生成一张专属古风矢量插画（SVG）。
输出：huyao-login-vue/public/treasures/<slug>.svg
风格：宣纸底 + 墨晕光轮 + 云纹双框 + 主体器物 + 题名 + 朱印，按四卷分主色。
"""
import os

# 相对脚本位置定位，项目整体移动后仍可直接重跑
OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "public", "treasures")
os.makedirs(OUT, exist_ok=True)

COLORS = {
    "tushan": ("#b03a3a", "#b8924a"),
    "daomen": ("#3f6f6b", "#c9a24a"),
    "xixiyu": ("#3f7fa6", "#d1a44a"),
    "nanguo": ("#7a4b8c", "#d1a44a"),
}

# slug -> (法宝名, 印章字, 卷, 主体 SVG 片段)
ITEMS = [
    ("xukong-zhilei", "虚空之泪", "泪", "tushan", """
<path d="M240 48 C282 106 306 136 306 166 A66 66 0 0 1 174 166 C174 136 198 106 240 48 Z" fill="url(#shine)" opacity=".92"/>
<path d="M240 48 C282 106 306 136 306 166 A66 66 0 0 1 174 166 C174 136 198 106 240 48 Z" fill="none" stroke="#8e2c2c" stroke-width="2.5"/>
<circle cx="240" cy="166" r="42" fill="#2c2340"/>
<circle cx="240" cy="166" r="42" fill="none" stroke="#d9b3e8" stroke-width="1" opacity=".7"/>
<path d="M204 168 Q240 128 276 168 Q240 204 204 168 Z" fill="none" stroke="#c58fe0" stroke-width="1.5" opacity=".8"/>
<g fill="#fff" opacity=".85"><circle cx="222" cy="150" r="2"/><circle cx="258" cy="145" r="1.5"/><circle cx="266" cy="181" r="2"/><circle cx="214" cy="184" r="1.5"/><circle cx="240" cy="166" r="2.5"/></g>
<path d="M120 76 l34 14 -24 26 M330 92 l-30 12 22 24" stroke="#b03a3a" stroke-width="2" fill="none" opacity=".65"/>
<circle cx="240" cy="166" r="74" fill="none" stroke="#b03a3a" stroke-width="1" stroke-dasharray="3 8" opacity=".5"/>
"""),
    ("chunai-tianpian", "纯爱天篇", "书", "tushan", """
<path d="M240 100 C208 80 172 80 138 92 L138 208 C172 196 208 196 240 216 Z" fill="#fdf8ea" stroke="#b8924a" stroke-width="2"/>
<path d="M240 100 C272 80 308 80 342 92 L342 208 C308 196 272 196 240 216 Z" fill="#f7efdc" stroke="#b8924a" stroke-width="2"/>
<path d="M240 100 V216" stroke="#8e2c2c" stroke-width="2"/>
<g stroke="#b8924a" stroke-width="1" opacity=".6"><path d="M154 112 H220 M154 128 H220 M154 144 H220 M260 112 H326 M260 128 H326 M260 144 H326"/></g>
<path d="M160 176 Q200 156 226 178 M254 178 Q280 156 320 176" fill="none" stroke="#b03a3a" stroke-width="1.6"/>
<g fill="#b03a3a" opacity=".8"><circle cx="200" cy="72" r="3"/><circle cx="280" cy="66" r="2.5"/><circle cx="312" cy="78" r="2"/></g>
<path d="M240 216 Q240 232 226 238" stroke="#b03a3a" stroke-width="1.5" fill="none"/>
"""),
    ("yimeng-chui", "忆梦锤", "锤", "tushan", """
<rect x="176" y="92" width="128" height="46" rx="10" fill="url(#wood)" stroke="#7a5a38" stroke-width="2"/>
<path d="M176 108 H304 M176 124 H304" stroke="#5a4430" stroke-width="1" opacity=".35"/>
<rect x="230" y="138" width="20" height="78" rx="5" fill="url(#wood)" stroke="#7a5a38" stroke-width="2"/>
<circle cx="240" cy="115" r="16" fill="none" stroke="#b03a3a" stroke-width="2"/>
<path d="M232 115 q8 -12 16 0 q-8 8 -16 0" fill="#b03a3a" opacity=".8"/>
<g stroke="#b8924a" stroke-width="1.5"><path d="M320 70 l8 18 18 8 -18 8 -8 18 -8 -18 -18 -8 18 -8 Z" fill="#f4e3b0"/><path d="M140 150 l6 12 12 6 -12 6 -6 12 -6 -12 -12 -6 12 -6 Z" fill="#f4e3b0"/></g>
"""),
    ("wujin-jiuhu", "无尽酒葫", "酒", "tushan", """
<path d="M232 62 h16 v18 c0 10 34 26 34 58 0 34 -18 62 -42 62 s-42 -28 -42 -62 c0 -32 34 -48 34 -58 Z" fill="url(#gold)" stroke="#8a6a2c" stroke-width="2"/>
<rect x="226" y="54" width="28" height="12" rx="3" fill="#7a5a38"/>
<path d="M198 150 Q240 138 282 150 M196 166 Q240 154 284 166" stroke="#fdf3d8" stroke-width="2" fill="none" opacity=".7"/>
<circle cx="222" cy="120" r="9" fill="#fff" opacity=".35"/>
<g stroke="#3f7fa6" stroke-width="1.5" fill="none" opacity=".55"><path d="M150 110 q18 -10 34 0"/><path d="M296 110 q18 -10 34 0"/><path d="M158 130 q16 -8 28 0"/></g>
<text x="240" y="192" text-anchor="middle" font-size="26" font-family="STKaiti,KaiTi,serif" fill="#8e2c2c">酒</text>
"""),
    ("jiuzhuan-xuanyinshui", "九转玄阴水", "阴", "tushan", """
<circle cx="240" cy="126" r="46" fill="#eaf3f6" stroke="#3f6f8a" stroke-width="2"/>
<path d="M252 84 A46 46 0 1 0 252 168 A36 36 0 1 1 252 84 Z" fill="#2f5b78" opacity=".85"/>
<g fill="none" stroke="#3f7fa6" stroke-width="2" opacity=".75"><path d="M150 186 q30 -16 60 0 t60 0 t60 0"/><path d="M150 202 q30 -16 60 0 t60 0 t60 0"/><path d="M160 218 q26 -12 52 0 t52 0 t52 0"/></g>
<g stroke="#8fb9d0" stroke-width="1.5"><path d="M240 52 V38 M240 52 l-12 -12 M240 52 l12 -12"/></g>
<path d="M300 70 l6 14 14 6 -14 6 -6 14 -6 -14 -14 -6 14 -6 Z" fill="#bfe0ee" opacity=".85"/>
"""),
    ("jueyuan-zhizhua", "绝缘之爪", "爪", "tushan", """
<circle cx="240" cy="140" r="76" fill="none" stroke="#b03a3a" stroke-width="2" stroke-dasharray="6 10" opacity=".6"/>
<path d="M196 196 C186 168 188 142 196 124 C202 110 214 108 218 122 L224 142 C226 120 238 112 244 126 L248 146 C252 122 266 118 270 134 L272 154 C282 142 296 150 290 166 C284 186 268 200 250 206 Z" fill="url(#gold)" stroke="#8e2c2c" stroke-width="2"/>
<g stroke="#8e2c2c" stroke-width="1.2" opacity=".6"><path d="M208 176 q18 -10 36 -6 M214 190 q18 -8 34 -4"/></g>
<g stroke="#b03a3a" stroke-width="1.5" fill="none" opacity=".7"><path d="M120 100 q40 20 60 40"/><path d="M360 100 q-40 20 -60 40"/><path d="M138 186 q34 -12 56 -6"/><path d="M342 186 q-34 -12 -56 -6"/></g>
<circle cx="240" cy="152" r="10" fill="#fff" opacity=".5"/>
"""),
    ("kuqing-shu", "苦情树", "树", "tushan", """
<path d="M232 216 C230 176 226 150 214 126 M248 216 C250 176 254 150 266 126" stroke="#7a5a38" stroke-width="10" fill="none" stroke-linecap="round"/>
<path d="M240 150 L180 108 M240 150 L300 108 M240 132 L210 88 M240 132 L272 88" stroke="#7a5a38" stroke-width="5" fill="none" stroke-linecap="round"/>
<circle cx="240" cy="112" r="72" fill="#e8b9b0" opacity=".55"/>
<g fill="#f7d9d2" opacity=".95"><circle cx="180" cy="90" r="8"/><circle cx="300" cy="90" r="8"/><circle cx="240" cy="58" r="9"/><circle cx="204" cy="66" r="6"/><circle cx="276" cy="66" r="6"/><circle cx="160" cy="130" r="6"/><circle cx="320" cy="130" r="6"/></g>
<g stroke="#b03a3a" stroke-width="1.8" fill="none"><path d="M150 200 Q240 168 330 200"/><path d="M156 214 Q240 184 324 214"/></g>
<g fill="#fff" opacity=".8"><circle cx="140" cy="72" r="3"/><circle cx="340" cy="72" r="3"/><circle cx="330" cy="52" r="2"/></g>
"""),
    ("wangquan-jian", "王权剑", "剑", "daomen", """
<g transform="rotate(-32 240 140)">
<path d="M236 48 L244 48 L248 176 L240 196 L232 176 Z" fill="url(#shine)" stroke="#8e2c2c" stroke-width="2"/>
<path d="M240 56 V188" stroke="#9aa5a5" stroke-width="1.2"/>
<rect x="214" y="176" width="52" height="10" rx="3" fill="url(#gold)" stroke="#8a6a2c"/>
<rect x="234" y="186" width="12" height="42" rx="3" fill="#7a5a38"/>
<circle cx="240" cy="232" r="6" fill="url(#gold)"/>
</g>
<text x="222" y="128" text-anchor="middle" font-family="STKaiti,KaiTi,serif" font-size="14" fill="#8e2c2c" transform="rotate(-32 222 128)">王权</text>
<g stroke="#b8924a" stroke-width="1.5" fill="none" opacity=".7"><path d="M120 180 q30 -30 54 -36"/><path d="M360 100 q-30 30 -54 36"/></g>
"""),
    ("chunzhi-yangyan", "纯质阳炎", "炎", "daomen", """
<circle cx="240" cy="146" r="78" fill="#f6c66a" opacity=".25" filter="url(#soft)"/>
<path d="M240 54 C266 92 296 112 296 158 C296 190 270 212 240 212 C210 212 184 190 184 158 C184 128 206 108 214 84 C220 108 232 112 232 92 C234 76 240 64 240 54 Z" fill="#e8762e" stroke="#b03a3a" stroke-width="2"/>
<path d="M240 108 C258 132 270 146 270 166 C270 186 256 200 240 200 C224 200 210 186 210 166 C210 148 224 136 230 122 C232 138 240 140 240 108 Z" fill="#f9d77a"/>
<path d="M240 148 C248 160 252 170 252 178 C252 190 246 196 240 196 C234 196 228 190 228 178 C228 170 234 162 240 148 Z" fill="#fff6d8"/>
<g stroke="#e8762e" stroke-width="2" fill="none" opacity=".7"><path d="M140 96 q-12 18 0 34"/><path d="M340 96 q12 18 0 34"/></g>
"""),
    ("zhuizong-fu", "追踪符", "符", "daomen", """
<path d="M204 52 H276 L276 224 L240 206 L204 224 Z" fill="#f7e6b0" stroke="#b8924a" stroke-width="2"/>
<g stroke="#b03a3a" stroke-width="2.5" fill="none" stroke-linecap="round"><path d="M240 70 V186"/><path d="M222 92 H258"/><path d="M226 112 q14 12 28 0"/><path d="M228 136 l24 20 M252 136 l-24 20"/><circle cx="240" cy="174" r="9"/></g>
<g stroke="#b8924a" stroke-width="1.5" fill="none" opacity=".7"><path d="M160 120 q-26 20 0 40"/><path d="M320 120 q26 20 0 40"/><path d="M300 160 l40 24"/></g>
<g fill="#b03a3a" opacity=".7"><circle cx="150" cy="86" r="3"/><circle cx="330" cy="90" r="3"/><circle cx="342" cy="196" r="2.5"/></g>
"""),
    ("zhengshen-huashen", "正神化身", "神", "daomen", """
<circle cx="240" cy="120" r="70" fill="#f4e3b0" opacity=".55"/>
<circle cx="240" cy="120" r="86" fill="none" stroke="#b8924a" stroke-width="1.5" stroke-dasharray="4 8"/>
<circle cx="240" cy="86" r="20" fill="#8a6a2c"/>
<path d="M210 112 C210 106 270 106 270 112 L292 200 C260 214 220 214 188 200 Z" fill="#6b5a44" stroke="#4a3b32" stroke-width="2"/>
<path d="M210 120 L166 168 M270 120 L314 168" stroke="#6b5a44" stroke-width="12" stroke-linecap="round"/>
<g stroke="#b8924a" stroke-width="2" fill="none" opacity=".8"><path d="M240 46 V32 M196 54 l-10 -10 M284 54 l10 -10"/></g>
<path d="M140 214 Q240 190 340 214" stroke="#b8924a" stroke-width="2" fill="none" opacity=".6"/>
"""),
    ("wujin-shalou", "无尽沙漏", "沙", "daomen", """
<path d="M186 62 H294 L294 78 L250 140 L294 202 L294 218 H186 L186 202 L230 140 L186 78 Z" fill="#e9f2ee" opacity=".7" stroke="#8a6a2c" stroke-width="2"/>
<rect x="176" y="52" width="128" height="12" rx="3" fill="url(#gold)" stroke="#8a6a2c"/>
<rect x="176" y="216" width="128" height="12" rx="3" fill="url(#gold)" stroke="#8a6a2c"/>
<path d="M196 76 H284 L244 130 H236 Z" fill="#d9b26a"/>
<path d="M236 150 H244 L284 204 H196 Z" fill="#c9a24a"/>
<path d="M240 130 V150" stroke="#b8924a" stroke-width="3"/>
<path d="M206 192 q12 -14 24 -6 q10 -12 20 -2 q10 -8 20 4" stroke="#3f6f6b" stroke-width="2" fill="none" opacity=".8"/>
<circle cx="240" cy="110" r="3" fill="#fff"/>
"""),
    ("tianluo-diwang", "天罗地网", "网", "daomen", """
<g stroke="#b8924a" stroke-width="1.5" fill="none" opacity=".85"><path d="M240 46 L340 140 L240 226 L140 140 Z"/><path d="M190 92 L290 188 M290 92 L190 188"/><path d="M240 46 V226 M140 140 H340"/><path d="M165 116 L315 164 M315 116 L165 164"/><circle cx="240" cy="140" r="62"/></g>
<circle cx="240" cy="140" r="12" fill="#b03a3a"/>
<g fill="#b03a3a"><circle cx="240" cy="46" r="4"/><circle cx="240" cy="226" r="4"/><circle cx="140" cy="140" r="4"/><circle cx="340" cy="140" r="4"/></g>
"""),
    ("yuyao-zimufu", "御妖子母符", "御", "daomen", """
<rect x="146" y="76" width="76" height="128" rx="4" fill="#f7e6b0" stroke="#b8924a" stroke-width="2" transform="rotate(-8 184 140)"/>
<rect x="258" y="76" width="76" height="128" rx="4" fill="#f7e6b0" stroke="#b8924a" stroke-width="2" transform="rotate(8 296 140)"/>
<g stroke="#b03a3a" stroke-width="2.5" fill="none"><path d="M170 106 V176 M184 100 V184 M198 106 V176 M282 106 V176 M296 100 V184 M310 106 V176"/></g>
<g stroke="#4a3b32" stroke-width="3" fill="none"><path d="M222 140 q18 -16 36 0"/><circle cx="222" cy="140" r="6"/><circle cx="258" cy="140" r="6"/></g>
<path d="M240 122 l8 18 -8 18 -8 -18 Z" fill="#b03a3a" opacity=".7"/>
"""),
    ("sixiang-jing", "四象镜", "镜", "daomen", """
<circle cx="240" cy="132" r="76" fill="url(#gold)" stroke="#8a6a2c" stroke-width="3"/>
<circle cx="240" cy="132" r="60" fill="#e9f2ee" stroke="#b8924a" stroke-width="2"/>
<g stroke="#3f6f6b" stroke-width="1.5" fill="none" opacity=".8"><circle cx="240" cy="132" r="46"/><path d="M240 86 V178 M194 132 H286"/><path d="M208 100 Q240 118 272 100 M208 164 Q240 146 272 164"/></g>
<g fill="#b03a3a"><circle cx="240" cy="100" r="4"/><circle cx="272" cy="132" r="4"/><circle cx="240" cy="164" r="4"/><circle cx="208" cy="132" r="4"/></g>
<path d="M224 208 H256 L262 232 H218 Z" fill="#7a5a38"/>
<circle cx="240" cy="132" r="76" fill="url(#shine)" opacity=".25"/>
"""),
    ("qiannian-yushuizhu", "千年御水珠", "珠", "xixiyu", """
<circle cx="240" cy="136" r="60" fill="#4a90c2" stroke="#2f5b78" stroke-width="2"/>
<circle cx="240" cy="136" r="60" fill="url(#shine)" opacity=".4"/>
<circle cx="222" cy="112" r="14" fill="#fff" opacity=".55"/>
<path d="M196 150 q22 -18 44 0 t44 0" stroke="#dff1fa" stroke-width="2" fill="none" opacity=".8"/>
<g stroke="#3f7fa6" stroke-width="2.5" fill="none"><path d="M120 190 q30 -22 60 0 t60 0 t60 0"/><path d="M130 210 q30 -20 60 0 t60 0 t60 0"/></g>
<path d="M240 76 V58 M228 62 l12 -12 12 12" stroke="#b8924a" stroke-width="2" fill="none"/>
"""),
    ("bingling-xueqiang", "冰凌雪枪", "冰", "xixiyu", """
<g transform="rotate(-30 240 140)">
<path d="M240 44 L254 84 L246 190 H234 L226 84 Z" fill="#dff1fa" stroke="#3f7fa6" stroke-width="2"/>
<path d="M240 44 V190" stroke="#fff" stroke-width="1.5" opacity=".8"/>
<rect x="226" y="186" width="28" height="10" rx="2" fill="url(#gold)"/>
<rect x="236" y="196" width="8" height="42" fill="#7a5a38"/>
</g>
<g stroke="#8fb9d0" stroke-width="1.5"><path d="M130 90 v24 M118 102 h24 M124 96 l12 12 M136 96 l-12 12"/><path d="M340 170 v24 M328 182 h24"/></g>
<circle cx="240" cy="140" r="88" fill="#bfe0ee" opacity=".18"/>
"""),
    ("qibao-miaoshupi", "七宝妙树皮", "木", "xixiyu", """
<path d="M186 64 Q240 50 294 64 L306 206 Q240 224 174 206 Z" fill="url(#wood)" stroke="#7a5a38" stroke-width="2"/>
<g stroke="#5a4430" stroke-width="1.5" fill="none" opacity=".7"><path d="M204 76 Q230 100 210 130 T220 196"/><path d="M240 68 Q258 108 240 146 T248 212"/><path d="M276 76 Q258 108 278 148 T270 200"/></g>
<g fill="#4a8c5a" opacity=".9"><path d="M306 130 q26 -18 34 4 q-26 12 -34 -4"/><path d="M174 150 q-26 -18 -34 4 q26 12 34 -4"/><path d="M240 54 q-8 -22 12 -26 q10 18 -12 26"/></g>
<g fill="#b8924a"><circle cx="226" cy="112" r="4"/><circle cx="262" cy="154" r="4"/><circle cx="212" cy="176" r="3"/></g>
"""),
    ("zihuan-jinling", "紫欢金铃", "铃", "nanguo", """
<path d="M196 168 C196 108 210 76 240 76 C270 76 284 108 284 168 C284 182 268 190 240 190 C212 190 196 182 196 168 Z" fill="url(#gold)" stroke="#8a6a2c" stroke-width="2"/>
<path d="M196 168 Q240 184 284 168" fill="none" stroke="#8a6a2c" stroke-width="2"/>
<circle cx="240" cy="70" r="10" fill="url(#gold)"/>
<path d="M240 80 V196" stroke="#8a6a2c" stroke-width="2"/>
<circle cx="240" cy="202" r="8" fill="#b03a3a"/>
<path d="M222 112 q18 12 36 0" stroke="#fff" stroke-width="2" fill="none" opacity=".55"/>
<g stroke="#7a4b8c" stroke-width="2" fill="none" opacity=".7"><path d="M164 108 q-18 32 0 64"/><path d="M148 96 q-28 44 0 88"/><path d="M316 108 q18 32 0 64"/><path d="M332 96 q28 44 0 88"/></g>
"""),
    ("fanchu-duzhen", "返雏毒针", "针", "nanguo", """
<g stroke="#c0c8c0" stroke-width="3" stroke-linecap="round"><path d="M180 70 L246 186"/><path d="M240 62 L286 178"/><path d="M300 74 L262 190"/></g>
<g fill="#7a4b8c"><circle cx="180" cy="70" r="5"/><circle cx="240" cy="62" r="5"/><circle cx="300" cy="74" r="5"/></g>
<circle cx="240" cy="160" r="52" fill="#9b6bb0" opacity=".2" filter="url(#soft)"/>
<g fill="#b03a3a" opacity=".85"><path d="M206 206 q0 -14 10 -14 q10 0 10 14 q0 10 -10 16 q-10 -6 -10 -16"/><path d="M262 210 q0 -12 8 -12 q8 0 8 12 q0 8 -8 14 q-8 -6 -8 -14"/></g>
"""),
    ("qinggu", "情蛊", "蛊", "nanguo", """
<path d="M240 200 C170 164 176 96 240 108 C304 96 310 164 240 200 Z" fill="#b03a3a" opacity=".85" stroke="#8e2c2c" stroke-width="2"/>
<path d="M240 186 C196 158 200 116 240 124 C280 116 284 158 240 186 Z" fill="#f4d9d2" opacity=".75"/>
<circle cx="240" cy="150" r="16" fill="#7a4b8c"/>
<path d="M240 136 q8 -14 16 0" stroke="#4a8c7a" stroke-width="2" fill="none"/>
<g stroke="#4a8c5a" stroke-width="2.5" fill="none"><path d="M240 200 Q214 224 196 216"/><path d="M240 200 Q266 224 284 216"/></g>
<g fill="#b03a3a" opacity=".6"><circle cx="164" cy="120" r="3"/><circle cx="316" cy="112" r="3"/><circle cx="330" cy="180" r="2.5"/></g>
<path d="M240 88 V72 M226 78 l14 -12 14 12" stroke="#b8924a" stroke-width="2" fill="none"/>
"""),
    ("zixinghe-shuijing", "紫星河水晶", "晶", "nanguo", """
<path d="M240 52 L286 128 L268 206 H212 L194 128 Z" fill="#9b6bb0" opacity=".85" stroke="#6a4380" stroke-width="2"/>
<path d="M240 52 V206 M194 128 H286" stroke="#d9b3e8" stroke-width="1.5" opacity=".7"/>
<path d="M240 52 L268 206 L212 206 Z" fill="#c79ad8" opacity=".45"/>
<path d="M168 92 L206 140 L192 206 H150 Z" fill="#7a4b8c" opacity=".7"/>
<path d="M312 92 L274 140 L288 206 H330 Z" fill="#7a4b8c" opacity=".7"/>
<g stroke="#b8924a" stroke-width="1.5" opacity=".7"><path d="M120 60 L150 96 M360 60 L330 96"/></g>
<g fill="#fff" opacity=".8"><circle cx="150" cy="72" r="2.5"/><circle cx="330" cy="72" r="2.5"/><circle cx="240" cy="40" r="2"/></g>
"""),
]

TEMPLATE = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 480 300" width="480" height="300" role="img" aria-label="{name}">
<defs>
<linearGradient id="paper" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#fcf7ea"/><stop offset=".55" stop-color="#f4e9d2"/><stop offset="1" stop-color="#e8d6b6"/></linearGradient>
<radialGradient id="halo"><stop offset="0" stop-color="{main}" stop-opacity=".32"/><stop offset=".7" stop-color="{main}" stop-opacity=".08"/><stop offset="1" stop-color="{main}" stop-opacity="0"/></radialGradient>
<linearGradient id="gold" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#f7e8bb"/><stop offset=".5" stop-color="#c9a24a"/><stop offset="1" stop-color="#8a6a2c"/></linearGradient>
<linearGradient id="wood" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#c8a878"/><stop offset="1" stop-color="#7a5a38"/></linearGradient>
<linearGradient id="shine" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#ffffff" stop-opacity=".95"/><stop offset="1" stop-color="#ffffff" stop-opacity=".1"/></linearGradient>
<filter id="soft" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="7"/></filter>
</defs>
<rect width="480" height="300" fill="url(#paper)"/>
<rect x="12" y="12" width="456" height="276" rx="10" fill="none" stroke="{accent}" stroke-opacity=".55" stroke-width="1.5"/>
<rect x="18" y="18" width="444" height="264" rx="8" fill="none" stroke="{accent}" stroke-opacity=".28" stroke-width="1" stroke-dasharray="4 7"/>
<circle cx="240" cy="138" r="112" fill="url(#halo)"/>
<circle cx="240" cy="138" r="82" fill="{main}" opacity=".08" filter="url(#soft)"/>
<path d="M20 226 Q90 200 150 226 T280 226 T460 226" stroke="{main}" stroke-opacity=".14" stroke-width="2" fill="none"/>
<path d="M20 238 Q90 212 150 238 T280 238 T460 238" stroke="{main}" stroke-opacity=".09" stroke-width="1.5" fill="none"/>
<g fill="{main}" opacity=".22"><circle cx="66" cy="64" r="3"/><circle cx="410" cy="58" r="2.5"/><circle cx="392" cy="212" r="3"/><circle cx="82" cy="196" r="2.5"/><circle cx="110" cy="44" r="1.8"/><circle cx="368" cy="40" r="1.8"/></g>
{body}
<text x="232" y="268" text-anchor="middle" font-family="STKaiti,KaiTi,STSong,serif" font-size="23" letter-spacing="4" fill="#8e2c2c">{name}</text>
<rect x="356" y="240" width="34" height="34" rx="4" fill="#b03a3a"/>
<text x="373" y="265" text-anchor="middle" font-family="STKaiti,KaiTi,STSong,serif" font-size="20" fill="#f5efe3">{mark}</text>
</svg>
"""

for slug, name, mark, scroll, body in ITEMS:
    main, accent = COLORS[scroll]
    svg = TEMPLATE.format(name=name, mark=mark, main=main, accent=accent, body=body.strip())
    with open(os.path.join(OUT, slug + ".svg"), "w", encoding="utf-8") as f:
        f.write(svg)

print("generated:", len(ITEMS), "svg files ->", OUT)
