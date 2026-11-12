# Product Backlog v1.0

| 故事ID | 用户故事描述 | 优先级 | Story Point | 状态 |
| :--- | :--- | :--- | :--- | :--- |
| PB-01 | 作为新同学，我可以用学号注册账号，以便参与问答社区。 | P0 | 3 | Done |
| PB-06 | 作为学生，我可以发布带标签、可关联课程的问题，以便向同学求助。 | P0 | 5 | Done |
| PB-07 | 作为用户，我可以分页浏览问题列表，以便发现待解决的问题。 | P0 | 3 | Done |
| PB-08 | 作为用户，我可以查看问题详情（渲染正文、回答、评论）。 | P0 | 5 | Done |
| PB-09 | 作为学生，我可以回答未删除的问题，以便帮助提问者。 | P0 | 3 | Done |
| PB-10 | 作为提问者或管理者，我可以采纳一个最佳答案，以便标记问题已解决。 | P0 | 5 | Done |
| PB-11 | 作为登录用户，我可以对他人的问题或回答点赞/点踩并可撤销，以便筛选优质内容。 | P0 | 3 | Done |
| PB-15 | 作为登录用户，我可以评论问题或回答并回复一层评论，以便追问细节。 | P0 | 3 | Done |
| PB-20 | 作为新用户，系统为我开立声誉账户，以便参与激励体系。 | P1 | 3 | Done |
| PB-21 | 作为回答者，回答被采纳时我获得 +15 声誉（自问自答不加分）。 | P1 | 5 | Done |
| PB-22 | 作为作者，被赞 +10、被踩 -2，且每日正向投票所得 ≤ 200。 | P1 | 5 | Done |
| PB-24 | 作为课程管理者，我可以为问题发起悬赏并冻结积分。 | P1 | 8 | Done |
| PB-31 | 作为用户，我可以用中英文关键词搜索并按标签/热度筛选。 | P1 | 5 | Done |
| PB-34 | 作为用户，我会收到新回答、被采纳、获得徽章等通知。 | P2 | 5 | Done |

## Gherkin 验收标准索引

| 故事ID | 验收标准文件 | 场景数 |
| :--- | :--- | :--- |
| PB-01 | [us-001-register.feature](stories/us-001-register.feature) | 3 |
| PB-06 | [us-003-ask.feature](stories/us-003-ask.feature) | 3 |
| PB-07 / PB-31 | [us-002-browse.feature](stories/us-002-browse.feature) | 4 |
| PB-08 | [pb-08-detail.feature](stories/pb-08-detail.feature) | 4 |
| PB-09 | [pb-09-answer.feature](stories/pb-09-answer.feature) | 4 |
| PB-10 | [pb-10-accept.feature](stories/pb-10-accept.feature) | 5 |
| PB-11 | [pb-11-vote.feature](stories/pb-11-vote.feature) | 4 |
| PB-15 | [pb-15-comment.feature](stories/pb-15-comment.feature) | 5 |

每个文件均覆盖快乐路径、边界路径与异常路径。
