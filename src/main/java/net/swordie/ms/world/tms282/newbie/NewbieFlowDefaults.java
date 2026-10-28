package net.swordie.ms.world.tms282.newbie;

/**
 * The built-in newbie flow configuration: used while {@code data/newbie/newbie-flow.json} does not exist (and as the
 * last valid version before the file was ever read). Must stay equal (as JSON) to the file shipped in the release
 * worktree {@code deploy/step7/newbie-flow.json}; tools/Tms282NewbieFlowCheck compares them.
 */
public final class NewbieFlowDefaults {
    private NewbieFlowDefaults() {
    }

    public static final String JSON = """
{
  "version": "2026-10-05.3",
  "enabled": true,
  "lobbyMap": 100000001,
  "lobbyPortal": 0,
  "speakerNpc": 9010000,
  "popupDelayMs": 2500,
  "resumeOnLogin": true,
  "recoveryMaps": [
    993166000,
    331001110,
    914000000,
    900010000,
    910150000,
    913070000,
    940001000,
    940011000,
    321000000,
    927030050,
    410000401,
    807100000,
    807100100
  ],
  "steps": [
    {
      "id": "spawn",
      "type": "Spawn",
      "default": "original",
      "byLoginJob": {}
    },
    {
      "id": "story",
      "type": "StoryDialog",
      "pages": [
        "歡迎來到#b楓之谷世界#k！\\r\\n我是負責照顧新冒險家的楓之谷GM。",
        "你已抵達冒險的起點。\\r\\n接下來我會幫你完成#b一轉#k、送你一份#b新手禮包#k，最後送你出發。",
        "如果中途關掉對話也沒關係，重新登入或回到新手大廳，我會從剛才的地方繼續。"
      ],
      "byLoginJob": {
        "KINESIS": [
          "歡迎來到#b楓之谷世界#k，凱內西斯！\\r\\n我是負責照顧新冒險家的楓之谷GM。",
          "你已抵達冒險的起點。\\r\\n接下來我會幫你完成#b一轉#k、送你一份#b新手禮包#k，最後送你出發。",
          "如果中途關掉對話也沒關係，重新登入或回到新手大廳，我會從剛才的地方繼續。"
        ]
      }
    },
    {
      "id": "firstJob",
      "type": "FirstJob",
      "levelTo": 10,
      "autoAssignAp": true,
      "chooseText": "請選擇你的職業（選了就不能改）：",
      "confirmText": "確定要成為#b{job}#k嗎？",
      "singleText": "現在幫你完成一轉，成為#b{job}#k。",
      "doneText": "恭喜！你現在是#b{job}#k了。",
      "byLoginJob": {
        "DUAL_BLADE": {
          "from": 0,
          "job": 400,
          "key": "thief",
          "label": "盜賊（影武者）",
          "quests": [
            1404
          ]
        },
        "CANNONER": {
          "from": 0,
          "job": 501,
          "key": "pirate",
          "label": "重砲手",
          "quests": [
            1405
          ]
        }
      },
      "jobs": [
        {
          "from": 0,
          "branches": [
            {
              "key": "warrior",
              "label": "劍士",
              "job": 100,
              "quests": [
                1401
              ]
            },
            {
              "key": "magician",
              "label": "法師",
              "job": 200,
              "quests": [
                1402
              ]
            },
            {
              "key": "bowman",
              "label": "弓箭手",
              "job": 300,
              "quests": [
                1403
              ]
            },
            {
              "key": "thief",
              "label": "盜賊",
              "job": 400,
              "quests": [
                1404
              ]
            },
            {
              "key": "pirate",
              "label": "海盜",
              "job": 500,
              "quests": [
                1405
              ]
            }
          ]
        },
        {
          "from": 1000,
          "branches": [
            {
              "key": "warrior",
              "label": "聖魂劍士",
              "job": 1100
            },
            {
              "key": "magician",
              "label": "烈焰巫師",
              "job": 1200
            },
            {
              "key": "bowman",
              "label": "破風使者",
              "job": 1300
            },
            {
              "key": "thief",
              "label": "暗夜行者",
              "job": 1400
            },
            {
              "key": "pirate",
              "label": "閃雷悍將",
              "job": 1500
            }
          ]
        },
        {
          "from": 3000,
          "branches": [
            {
              "key": "magician",
              "label": "煉獄巫師",
              "job": 3200
            },
            {
              "key": "crossbow",
              "label": "狂豹獵人",
              "job": 3300
            },
            {
              "key": "pirate",
              "label": "機甲戰神",
              "job": 3500
            },
            {
              "key": "*",
              "label": "爆拳槍神",
              "job": 3700
            }
          ]
        },
        {
          "from": 3001,
          "branches": [
            {
              "key": "*",
              "label": "惡魔殺手",
              "job": 3100
            },
            {
              "key": "*",
              "label": "惡魔復仇者",
              "job": 3101
            }
          ]
        },
        {
          "from": 2000,
          "branches": [
            {
              "key": "*",
              "label": "狂狼勇士",
              "job": 2100
            }
          ]
        },
        {
          "from": 2001,
          "branches": [
            {
              "key": "*",
              "label": "龍魔導士",
              "job": 2210
            }
          ]
        },
        {
          "from": 2002,
          "branches": [
            {
              "key": "*",
              "label": "精靈遊俠",
              "job": 2300
            }
          ]
        },
        {
          "from": 2003,
          "branches": [
            {
              "key": "*",
              "label": "幻影俠盜",
              "job": 2400
            }
          ]
        },
        {
          "from": 2004,
          "branches": [
            {
              "key": "*",
              "label": "夜光",
              "job": 2700
            }
          ]
        },
        {
          "from": 2005,
          "branches": [
            {
              "key": "*",
              "label": "隱月",
              "job": 2500
            }
          ]
        },
        {
          "from": 3002,
          "branches": [
            {
              "key": "*",
              "label": "傑諾",
              "job": 3600
            }
          ]
        },
        {
          "from": 4001,
          "branches": [
            {
              "key": "*",
              "label": "劍豪",
              "job": 4100
            }
          ]
        },
        {
          "from": 4002,
          "branches": [
            {
              "key": "*",
              "label": "陰陽師",
              "job": 4200
            }
          ]
        },
        {
          "from": 5000,
          "branches": [
            {
              "key": "*",
              "label": "米哈逸",
              "job": 5100
            }
          ]
        },
        {
          "from": 14000,
          "branches": [
            {
              "key": "*",
              "label": "凱內西斯",
              "job": 14200
            }
          ]
        },
        {
          "from": 15003,
          "branches": [
            {
              "key": "*",
              "label": "卡莉",
              "job": 15400
            }
          ]
        },
        {
          "from": 16001,
          "branches": [
            {
              "key": "*",
              "label": "菈菈",
              "job": 16200
            }
          ]
        }
      ]
    },
    {
      "id": "gift",
      "type": "StarterGift",
      "introText": "這是給新冒險家的#b新手禮包#k，每個角色只能領一次：",
      "confirmText": "要現在領取嗎？",
      "doneText": "禮包已經放進你的背包了，請查看背包。",
      "declinedText": "新手禮包是必領的，領取後才能繼續，馬上再為你開啟。",
      "fullText": "背包空間不夠。請先整理背包（裝備欄、消耗欄各留 6 格），再重新登入或回到新手大廳領取。",
      "limitText": "這個帳號領取新手禮包的次數已達上限，這個角色不會再發禮包。",
      "blockedText": "目前無法發放道具，請稍後重新登入再試。",
      "maxClaimsPerUser": 0,
      "meso": 500000,
      "nxPrepaid": 10000,
      "items": [
        {
          "itemId": 2000000,
          "quantity": 100
        },
        {
          "itemId": 2000003,
          "quantity": 100
        },
        {
          "itemId": 2030000,
          "quantity": 10
        },
        {
          "itemId": 2450042,
          "quantity": 3
        }
      ],
      "chooseText": "請選一隻#b永久騎寵#k，會和新手禮包一起發給你（雙擊騎寵券即可學會）：",
      "chooseOne": [
        {
          "itemId": 2433460,
          "quantity": 1
        },
        {
          "itemId": 2431881,
          "quantity": 1
        },
        {
          "itemId": 2639063,
          "quantity": 1
        },
        {
          "itemId": 2637787,
          "quantity": 1
        },
        {
          "itemId": 2638956,
          "quantity": 1
        },
        {
          "itemId": 2439649,
          "quantity": 1
        },
        {
          "itemId": 2634807,
          "quantity": 1
        },
        {
          "itemId": 2638599,
          "quantity": 1
        }
      ],
      "equipment": {
        "warrior": [
          {
            "itemId": 1302182,
            "quantity": 1
          },
          {
            "itemId": 1002043,
            "quantity": 1
          },
          {
            "itemId": 1040038,
            "quantity": 1,
            "gender": 0
          },
          {
            "itemId": 1041064,
            "quantity": 1,
            "gender": 1
          },
          {
            "itemId": 1072668,
            "quantity": 1
          }
        ],
        "magician": [
          {
            "itemId": 1382100,
            "quantity": 1
          },
          {
            "itemId": 1002017,
            "quantity": 1
          },
          {
            "itemId": 1040004,
            "quantity": 1,
            "gender": 0
          },
          {
            "itemId": 1041016,
            "quantity": 1,
            "gender": 1
          },
          {
            "itemId": 1072045,
            "quantity": 1
          }
        ],
        "bowman": [
          {
            "itemId": 1452051,
            "quantity": 1
          },
          {
            "itemId": 2060000,
            "quantity": 1000
          },
          {
            "itemId": 1002057,
            "quantity": 1
          },
          {
            "itemId": 1040071,
            "quantity": 1,
            "gender": 0
          },
          {
            "itemId": 1041007,
            "quantity": 1,
            "gender": 1
          },
          {
            "itemId": 1072059,
            "quantity": 1
          }
        ],
        "crossbow": [
          {
            "itemId": 1462092,
            "quantity": 1
          },
          {
            "itemId": 2061000,
            "quantity": 1000
          },
          {
            "itemId": 1002057,
            "quantity": 1
          },
          {
            "itemId": 1040071,
            "quantity": 1,
            "gender": 0
          },
          {
            "itemId": 1041007,
            "quantity": 1,
            "gender": 1
          },
          {
            "itemId": 1072059,
            "quantity": 1
          }
        ],
        "thief": [
          {
            "itemId": 1332063,
            "quantity": 1
          },
          {
            "itemId": 1472061,
            "quantity": 1
          },
          {
            "itemId": 2070000,
            "quantity": 500
          },
          {
            "itemId": 1002126,
            "quantity": 1
          },
          {
            "itemId": 1040031,
            "quantity": 1,
            "gender": 0
          },
          {
            "itemId": 1041037,
            "quantity": 1,
            "gender": 1
          },
          {
            "itemId": 1072070,
            "quantity": 1
          }
        ],
        "pirate": [
          {
            "itemId": 1482014,
            "quantity": 1
          },
          {
            "itemId": 1492014,
            "quantity": 1
          },
          {
            "itemId": 2330006,
            "quantity": 600
          },
          {
            "itemId": 1002610,
            "quantity": 1
          },
          {
            "itemId": 1052095,
            "quantity": 1
          },
          {
            "itemId": 1072366,
            "quantity": 1
          }
        ],
        "*": [
          {
            "itemId": 1002019,
            "quantity": 1
          },
          {
            "itemId": 1052165,
            "quantity": 1
          },
          {
            "itemId": 1072366,
            "quantity": 1
          }
        ]
      }
    },
    {
      "id": "warp",
      "type": "WarpMenu",
      "text": "準備好了嗎？要去哪裡？",
      "stayLabel": "先留在新手大廳",
      "stayText": "好的。下次登入或回到新手大廳時，我會再問你一次。",
      "destinations": [
        {
          "label": "前往主城：弓箭手村",
          "map": 100000000,
          "portal": 0
        },
        {
          "label": "前往推薦練功地圖：弓箭手村北邊山丘",
          "map": 100010000,
          "portal": 0
        }
      ]
    }
  ]
}
""";
}
