import { useState } from 'react';
import { api } from '../api';
import { useAuth } from '../auth';

/**
 * 我的资料（PB-41）：只允许改学院与班级。
 * 学号、昵称、邮箱是身份凭据，由注册流程与管理端负责，这里只读展示。
 */
export function ProfilePage() {
  const { me, refresh } = useAuth();
  const [college, setCollege] = useState(me?.college ?? '');
  const [className, setClassName] = useState(me?.className ?? '');
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  if (!me) return null; // 路由外层 RequireAuth 已兜底，这里只做类型收窄

  const save = async () => {
    setSaving(true);
    setError('');
    setMessage('');
    try {
      await api.updateProfile({ college, className });
      await refresh(); // 同步头部与其它页面用的 me
      setMessage('已保存');
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="form-page">
      <h1>我的资料</h1>
      <p className="muted">学号与昵称用于登录和 @提及，不能在这里修改；如需变更请联系课程管理员。</p>
      <label>学号 / 工号
        <input value={me.username} readOnly disabled />
      </label>
      <label>昵称
        <input value={me.displayName} readOnly disabled />
      </label>
      <label>学院（≤50 字）
        <input value={college} maxLength={50} onChange={(e) => setCollege(e.target.value)} />
      </label>
      <label>班级（≤50 字）
        <input value={className} maxLength={50} onChange={(e) => setClassName(e.target.value)} />
      </label>
      {error && <p className="error">{error}</p>}
      {message && <p className="muted">{message}</p>}
      <button className="primary" disabled={saving} onClick={save}>
        {saving ? '保存中…' : '保存'}
      </button>
    </div>
  );
}
