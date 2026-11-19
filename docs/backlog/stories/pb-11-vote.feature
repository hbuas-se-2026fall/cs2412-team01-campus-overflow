Feature: 投票、改投与撤销
  作为登录用户
  我想要对问题或回答点赞或点踩
  以便于让优质内容排在前面

  Scenario: 快乐路径 - 点赞、重复点赞、改为点踩
    Given 小刚已经登录系统
    And 小红的回答得分为 0
    When 小刚对该回答点赞
    Then 回答得分变为 1
    When 小刚再次点赞
    Then 回答得分仍为 1
    When 小刚改为点踩
    Then 回答得分变为 -1

  Scenario: 快乐路径 - 撤销投票
    Given 小刚已对小红的回答点赞，得分为 1
    When 小刚撤销投票
    Then 回答得分变为 0

  Scenario: 异常路径 - 不能给自己的内容投票
    Given 小刚是问题作者
    When 小刚给自己的问题点赞
    Then 系统返回 422，错误码为 "QA-1015"
    And 提示信息包含 "自己"

  Scenario: 边界路径 - 非法投票值
    Given 小刚已经登录系统
    When 小刚提交投票值 2
    Then 系统返回 422
    And 回答得分不变
