"""Interactive local bootstrap for an empty fan database. No credential is written to a file."""
import getpass
import re
import sys
try:
    import bcrypt
    import mysql.connector
except ImportError:
    sys.exit('请先安装 bcrypt 和 mysql-connector-python。')

name = input('初始管理员用户名：').strip()
if not re.fullmatch(r'[A-Za-z0-9_.-]{2,30}', name):
    sys.exit('用户名须为 2 至 30 位字母、数字、下划线、点或连字符。')
password = getpass.getpass('初始管理员密码（至少 12 位）：')
if len(password) < 12 or len(password.encode('utf-8')) > 72:
    sys.exit('密码须至少 12 位，且 UTF-8 编码不得超过 72 字节。')
if password != getpass.getpass('再次输入密码：'):
    sys.exit('两次密码不一致。')
db_user = input('数据库用户名（需有 fan 表读写权限）：').strip()
db_password = getpass.getpass('数据库密码：')
password_hash = bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt()).decode('ascii')
connection = mysql.connector.connect(host='127.0.0.1', port=13306, user=db_user, password=db_password, database='fan', autocommit=False)
try:
    cursor = connection.cursor()
    cursor.execute('SELECT user_id FROM sys_user WHERE user_id = %s OR user_name = %s FOR UPDATE', (1, name))
    if cursor.fetchone():
        raise RuntimeError('管理员 ID 或用户名已存在；未修改现有账号。')
    cursor.execute('INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, password, status, del_flag, create_by, create_time, pwd_update_date) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,NOW(),NOW())', (1,103,name,name,password_hash,'0','0','bootstrap'))
    cursor.execute('INSERT INTO sys_user_role (user_id,role_id) VALUES (%s,%s)', (1,1))
    connection.commit()
    print('初始管理员已创建。')
except Exception:
    connection.rollback()
    raise
finally:
    connection.close()
