/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package java.util;

import java.io.Serializable;
import java.io.IOException;
import java.security.*;
import java.util.Map;
import java.util.HashMap;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Collections;
import java.io.ObjectStreamField;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import sun.security.util.SecurityConstants;

/**
 * This class is for property permissions.
 *
 * <P>
 * The name is the name of the property ("java.home",
 * "os.name", etc). The naming
 * convention follows the  hierarchical property naming convention.
 * Also, an asterisk
 * may appear at the end of the name, following a ".", or by itself, to
 * signify a wildcard match. For example: "java.*" and "*" signify a wildcard
 * match, while "*java" and "a*b" do not.
 * <P>
 * The actions to be granted are passed to the constructor in a string containing
 * a list of one or more comma-separated keywords. The possible keywords are
 * "read" and "write". Their meaning is defined as follows:
 *
 * <DL>
 *    <DT> read
 *    <DD> read permission. Allows <code>System.getProperty</code> to
 *         be called.
 *    <DT> write
 *    <DD> write permission. Allows <code>System.setProperty</code> to
 *         be called.
 * </DL>
 * <P>
 * The actions string is converted to lowercase before processing.
 * <P>
 * Care should be taken before granting code permission to access
 * certain system properties.  For example, granting permission to
 * access the "java.home" system property gives potentially malevolent
 * code sensitive information about the system environment (the Java
 * installation directory).  Also, granting permission to access
 * the "user.name" and "user.home" system properties gives potentially
 * malevolent code sensitive information about the user environment
 * (the user's account name and home directory).
 *
 * @see java.security.BasicPermission
 * @see java.security.Permission
 * @see java.security.Permissions
 * @see java.security.PermissionCollection
 * @see java.lang.SecurityManager
 *
 *
 * @author Roland Schemers
 * @since 1.2
 *
 * @serial exclude
 */

public final class PropertyPermission extends BasicPermission {

    //0x是十六进制的前缀
    /**
     * Read action.
     */
    private final static int READ    = 0x1; //1

    /**
     * Write action.
     */
    private final static int WRITE   = 0x2; //2
    /**
     * All actions (read,write);
     */
    private final static int ALL     = READ|WRITE; //0x1与0x2按位或 值为3
    /**
     * No actions.
     */
    private final static int NONE    = 0x0; //0

    /**
     * The actions mask.
     *
     */
    private transient int mask;

    /**
     * The actions string.
     *
     * @serial
     */
    private String actions; // Left null as long as possible, then
                            // created and re-used in the getAction function.

    /**
     * initialize a PropertyPermission object. Common to all constructors.
     * Also called during de-serialization.
     *
     * @param mask the actions mask to use.
     *
     */
    private void init(int mask) {
        //初始化方法
        if ((mask & ALL) != mask) //按位与，0&3 = 0，1&3 = 1，2&3 = 2，3&3 = 3
            // 故若不等于抛出异常
            throw new IllegalArgumentException("invalid actions mask");

        if (mask == NONE) //mask等于0
            //抛出异常
            throw new IllegalArgumentException("invalid actions mask");

        //两次if足以说明mask的值合法
        if (getName() == null) //获取名字
            //否则抛出异常
            throw new NullPointerException("name can't be null");

        this.mask = mask; //将mask赋值
    }

    /**
     * Creates a new PropertyPermission object with the specified name.
     * The name is the name of the system property, and
     * <i>actions</i> contains a comma-separated list of the
     * desired actions granted on the property. Possible actions are
     * "read" and "write".
     *
     * @param name the name of the PropertyPermission.
     * @param actions the actions string.
     *
     * @throws NullPointerException if <code>name</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>name</code> is empty or if
     * <code>actions</code> is invalid.
     */
    public PropertyPermission(String name, String actions) {
        super(name,actions); //调用父类构造函数
        init(getMask(actions)); //初始化代码化的actions
    }

    /**
     * Checks if this PropertyPermission object "implies" the specified
     * permission.
     * <P>
     * More specifically, this method returns true if:
     * <ul>
     * <li> <i>p</i> is an instanceof PropertyPermission,
     * <li> <i>p</i>'s actions are a subset of this
     * object's actions, and
     * <li> <i>p</i>'s name is implied by this object's
     *      name. For example, "java.*" implies "java.home".
     * </ul>
     * @param p the permission to check against.
     *
     * @return true if the specified permission is implied by this object,
     * false if not.
     */
    public boolean implies(Permission p) {
        if (!(p instanceof PropertyPermission))
            return false;

        PropertyPermission that = (PropertyPermission) p;

        // we get the effective mask. i.e., the "and" of this and that.
        // They must be equal to that.mask for implies to return true.

        return ((this.mask & that.mask) == that.mask) && super.implies(that);
    }

    /**
     * Checks two PropertyPermission objects for equality. Checks that <i>obj</i> is
     * a PropertyPermission, and has the same name and actions as this object.
     * <P>
     * @param obj the object we are testing for equality with this object.
     * @return true if obj is a PropertyPermission, and has the same name and
     * actions as this PropertyPermission object.
     */
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (! (obj instanceof PropertyPermission))
            return false;

        PropertyPermission that = (PropertyPermission) obj;

        return (this.mask == that.mask) &&
            (this.getName().equals(that.getName()));
    }

    /**
     * Returns the hash code value for this object.
     * The hash code used is the hash code of this permissions name, that is,
     * <code>getName().hashCode()</code>, where <code>getName</code> is
     * from the Permission superclass.
     *
     * @return a hash code value for this object.
     */
    public int hashCode() {
        return this.getName().hashCode();
    }

    /**
     * Converts an actions String to an actions mask.
     *
     * @param actions the action string.
     * @return the actions mask.
     */
    private static int getMask(String actions) {

        int mask = NONE; //重置mask值为空的数值化值

        if (actions == null) {
            return mask; //若没有操作则直接返回数值化后的空
        }

        // Use object identity comparison against known-interned strings for
        // performance benefit (these values are used heavily within the JDK).
        if (actions == SecurityConstants.PROPERTY_READ_ACTION) { //若操作为read
            return READ; //返回数值化后的读取
        } if (actions == SecurityConstants.PROPERTY_WRITE_ACTION) { //若操作为write
            return WRITE; //返回数值化后的写入
        } else if (actions == SecurityConstants.PROPERTY_RW_ACTION) { //若操作为read,write
            return READ|WRITE; //返回数值化后的读写
        }

        //以上if都没有通过

        char[] a = actions.toCharArray(); //将actions字符串转为字符数组

        int i = a.length - 1; //得到长度减1的值

        if (i < 0) //若i值为-1，说明a.length为0，长度判空
            return mask; //返回数值化后的空

        while (i != -1) { //开启循环
            char c; //声明字符变量c

            // skip whitespace
            //筛选掉空格，换行，缩进
            while ((i!=-1) && ((c = a[i]) == ' ' ||
                    c == '\r' ||
                    c == '\n' ||
                    c == '\f' ||
                    c == '\t'))
                i--;

            // check for the known strings
            //筛选是否存在已知字符
            //一定要注意，还有一种可能是两个都存在，比如"readwrite"
            //ALL的值为READ和WRITE的按位或
            int matchlen; //声明一个变量，存放长度

            if (i >= 3 && (a[i-3] == 'r' || a[i-3] == 'R') &&
                    (a[i-2] == 'e' || a[i-2] == 'E') &&
                    (a[i-1] == 'a' || a[i-1] == 'A') &&
                    (a[i] == 'd' || a[i] == 'D'))
            //监测到read或者READ
            {
                matchlen = 4; //赋值
                mask |= READ; //按位或 赋予mask 按位或保证是否为ALL

            } else if (i >= 4 && (a[i-4] == 'w' || a[i-4] == 'W') &&
                    (a[i-3] == 'r' || a[i-3] == 'R') &&
                    (a[i-2] == 'i' || a[i-2] == 'I') &&
                    (a[i-1] == 't' || a[i-1] == 'T') &&
                    (a[i] == 'e' || a[i] == 'E'))
            //监测到write或者WRITE
            {
                matchlen = 5; //赋值
                mask |= WRITE; //按位或 赋予mask 按位或保证是否为ALL

            } else {
                // parse error
                //抛出解析错误异常
                throw new IllegalArgumentException(
                        "invalid permission: " + actions);
                //无效的许可：***
            }

            // make sure we didn't just match the tail of a word
            // like "ackbarfaccept".  Also, skip to the comma.
            //现在知道末尾存在read或writh的大或小写，需要检查前面的字符
            boolean seencomma = false; //声明布尔类变量
            while (i >= matchlen && !seencomma) { //循环检查之前的每个字符
                switch(a[i-matchlen]) {
                    case ',':
                        seencomma = true; //更改循环条件值，停止循环
                        break; //若为逗号说明这段结束，跳出循环
                    case ' ': case '\r': case '\n':
                    case '\f': case '\t':
                        break; //若存在以上符号，说明需要继续向前判断
                    default:
                        throw new IllegalArgumentException(
                                "invalid permission: " + actions);
                        //出现了其它的字符说明参数不合法，抛出解析错误异常
                }
                i--; //自减，循环条件
            }

            // point i at the location of the comma minus one (or -1).
            i -= matchlen; //减去获得的字符长度，循环直到i=-1
        }
        return mask; //返回mask值
    }


    /**
     * Return the canonical string representation of the actions.
     * Always returns present actions in the following order:
     * read, write.
     *
     * @return the canonical string representation of the actions.
     */
    static String getActions(int mask) {
        StringBuilder sb = new StringBuilder();
        boolean comma = false;

        if ((mask & READ) == READ) {
            comma = true;
            sb.append("read");
        }

        if ((mask & WRITE) == WRITE) {
            if (comma) sb.append(',');
            else comma = true;
            sb.append("write");
        }
        return sb.toString();
    }

    /**
     * Returns the "canonical string representation" of the actions.
     * That is, this method always returns present actions in the following order:
     * read, write. For example, if this PropertyPermission object
     * allows both write and read actions, a call to <code>getActions</code>
     * will return the string "read,write".
     *
     * @return the canonical string representation of the actions.
     */
    public String getActions() {
        if (actions == null)
            actions = getActions(this.mask);

        return actions;
    }

    /**
     * Return the current action mask.
     * Used by the PropertyPermissionCollection
     *
     * @return the actions mask.
     */
    int getMask() {
        return mask;
    }

    /**
     * Returns a new PermissionCollection object for storing
     * PropertyPermission objects.
     * <p>
     *
     * @return a new PermissionCollection object suitable for storing
     * PropertyPermissions.
     */
    public PermissionCollection newPermissionCollection() {
        return new PropertyPermissionCollection();
    }


    private static final long serialVersionUID = 885438825399942851L;

    /**
     * WriteObject is called to save the state of the PropertyPermission
     * to a stream. The actions are serialized, and the superclass
     * takes care of the name.
     */
    private synchronized void writeObject(java.io.ObjectOutputStream s)
        throws IOException
    {
        // Write out the actions. The superclass takes care of the name
        // call getActions to make sure actions field is initialized
        if (actions == null)
            getActions();
        s.defaultWriteObject();
    }

    /**
     * readObject is called to restore the state of the PropertyPermission from
     * a stream.
     */
    private synchronized void readObject(java.io.ObjectInputStream s)
         throws IOException, ClassNotFoundException
    {
        // Read in the action, then initialize the rest
        s.defaultReadObject();
        init(getMask(actions));
    }
}

/**
 * A PropertyPermissionCollection stores a set of PropertyPermission
 * permissions.
 *
 * @see java.security.Permission
 * @see java.security.Permissions
 * @see java.security.PermissionCollection
 *
 *
 * @author Roland Schemers
 *
 * @serial include
 */
final class PropertyPermissionCollection extends PermissionCollection
    implements Serializable
{

    /**
     * Key is property name; value is PropertyPermission.
     * Not serialized; see serialization section at end of class.
     */
    private transient Map<String, PropertyPermission> perms;

    /**
     * Boolean saying if "*" is in the collection.
     *
     * @see #serialPersistentFields
     */
    // No sync access; OK for this to be stale.
    private boolean all_allowed;

    /**
     * Create an empty PropertyPermissionCollection object.
     */
    public PropertyPermissionCollection() {
        perms = new HashMap<>(32);     // Capacity for default policy
        all_allowed = false;
    }

    /**
     * Adds a permission to the PropertyPermissions. The key for the hash is
     * the name.
     *
     * @param permission the Permission object to add.
     *
     * @exception IllegalArgumentException - if the permission is not a
     *                                       PropertyPermission
     *
     * @exception SecurityException - if this PropertyPermissionCollection
     *                                object has been marked readonly
     */
    public void add(Permission permission) {
        if (! (permission instanceof PropertyPermission))
            throw new IllegalArgumentException("invalid permission: "+
                                               permission);
        if (isReadOnly())
            throw new SecurityException(
                "attempt to add a Permission to a readonly PermissionCollection");

        PropertyPermission pp = (PropertyPermission) permission;
        String propName = pp.getName();

        synchronized (this) {
            PropertyPermission existing = perms.get(propName);

            if (existing != null) {
                int oldMask = existing.getMask();
                int newMask = pp.getMask();
                if (oldMask != newMask) {
                    int effective = oldMask | newMask;
                    String actions = PropertyPermission.getActions(effective);
                    perms.put(propName, new PropertyPermission(propName, actions));
                }
            } else {
                perms.put(propName, pp);
            }
        }

        if (!all_allowed) {
            if (propName.equals("*"))
                all_allowed = true;
        }
    }

    /**
     * Check and see if this set of permissions implies the permissions
     * expressed in "permission".
     *
     * @param permission the Permission object to compare
     *
     * @return true if "permission" is a proper subset of a permission in
     * the set, false if not.
     */
    public boolean implies(Permission permission) {
        if (! (permission instanceof PropertyPermission))
                return false;

        PropertyPermission pp = (PropertyPermission) permission;
        PropertyPermission x;

        int desired = pp.getMask();
        int effective = 0;

        // short circuit if the "*" Permission was added
        if (all_allowed) {
            synchronized (this) {
                x = perms.get("*");
            }
            if (x != null) {
                effective |= x.getMask();
                if ((effective & desired) == desired)
                    return true;
            }
        }

        // strategy:
        // Check for full match first. Then work our way up the
        // name looking for matches on a.b.*

        String name = pp.getName();
        //System.out.println("check "+name);

        synchronized (this) {
            x = perms.get(name);
        }

        if (x != null) {
            // we have a direct hit!
            effective |= x.getMask();
            if ((effective & desired) == desired)
                return true;
        }

        // work our way up the tree...
        int last, offset;

        offset = name.length()-1;

        while ((last = name.lastIndexOf(".", offset)) != -1) {

            name = name.substring(0, last+1) + "*";
            //System.out.println("check "+name);
            synchronized (this) {
                x = perms.get(name);
            }

            if (x != null) {
                effective |= x.getMask();
                if ((effective & desired) == desired)
                    return true;
            }
            offset = last -1;
        }

        // we don't have to check for "*" as it was already checked
        // at the top (all_allowed), so we just return false
        return false;
    }

    /**
     * Returns an enumeration of all the PropertyPermission objects in the
     * container.
     *
     * @return an enumeration of all the PropertyPermission objects.
     */
    @SuppressWarnings("unchecked")
    public Enumeration<Permission> elements() {
        // Convert Iterator of Map values into an Enumeration
        synchronized (this) {
            /**
             * Casting to rawtype since Enumeration<PropertyPermission>
             * cannot be directly cast to Enumeration<Permission>
             */
            return (Enumeration)Collections.enumeration(perms.values());
        }
    }

    private static final long serialVersionUID = 7015263904581634791L;

    // Need to maintain serialization interoperability with earlier releases,
    // which had the serializable field:
    //
    // Table of permissions.
    //
    // @serial
    //
    // private Hashtable permissions;
    /**
     * @serialField permissions java.util.Hashtable
     *     A table of the PropertyPermissions.
     * @serialField all_allowed boolean
     *     boolean saying if "*" is in the collection.
     */
    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("permissions", Hashtable.class),
        new ObjectStreamField("all_allowed", Boolean.TYPE),
    };

    /**
     * @serialData Default fields.
     */
    /*
     * Writes the contents of the perms field out as a Hashtable for
     * serialization compatibility with earlier releases. all_allowed
     * unchanged.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        // Don't call out.defaultWriteObject()

        // Copy perms into a Hashtable
        Hashtable<String, Permission> permissions =
            new Hashtable<>(perms.size()*2);
        synchronized (this) {
            permissions.putAll(perms);
        }

        // Write out serializable fields
        ObjectOutputStream.PutField pfields = out.putFields();
        pfields.put("all_allowed", all_allowed);
        pfields.put("permissions", permissions);
        out.writeFields();
    }

    /*
     * Reads in a Hashtable of PropertyPermissions and saves them in the
     * perms field. Reads in all_allowed.
     */
    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        // Don't call defaultReadObject()

        // Read in serialized fields
        ObjectInputStream.GetField gfields = in.readFields();

        // Get all_allowed
        all_allowed = gfields.get("all_allowed", false);

        // Get permissions
        @SuppressWarnings("unchecked")
        Hashtable<String, PropertyPermission> permissions =
            (Hashtable<String, PropertyPermission>)gfields.get("permissions", null);
        perms = new HashMap<>(permissions.size()*2);
        perms.putAll(permissions);
    }
}
