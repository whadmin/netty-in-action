package com.phei.netty.codec.messagePack.demo;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.codec.binary.Base64;
import org.msgpack.MessagePack;
import org.msgpack.annotation.Message;
import org.msgpack.template.SetTemplate;
import org.msgpack.template.Template;
import org.msgpack.template.Templates;

/**
 * <Description functions in a word> 关于Msgpack官网上是这么说的，It's like JSON.but fast and small.<BR />
 * Msgpack0.6.12 使用总结：<BR />
 * 依赖包：javassist-3.18.1-GA.jar，msgpack-0.6.12.jar<BR />
 * 1.单个对象使用Msgpack <BR />
 * 1.1 此对象必须要实现Serializable接口(java.io.Serializable) <BR />
 * 1.2 此对象必须要使用@Message注解(org.msgpack.annotation.Message) <BR />
 * 1.3 此对象属性中不能有用transient修饰的字段. <BR />
 * 2.序列化List，Map(List接口)直接这么做msgpack不支持. <BR />
 * 2.1 构建含有List,Map属性的字段对象 <BR />
 * 2.2 该有的步骤和构建单个对象使用Msgpack一样。<BR />
 * <Detail description>
 * 
 * @author Peter.Qiu
 * @version [Version NO, 2015-12-16]
 * @see [Related classes/methods]
 * @since [product/module version]
 */
public class MsgpackTest {

    public static void main(String[] args) {
        MessagePack pack = new MessagePack();
        User user = new User(1, "name", "password");
        try {
            System.out.println("单个对象使用Msgpack");
            System.out.println("序列化前: " + user.toString());
            // 序列化
            byte[] bytes = pack.write(user);
            // 反序列化
            User s = pack.read(bytes, User.class);
            System.out.println("反序列化: " + s.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println();
        // ArrayList
        try {
            System.out.println("ArrayList使用Msgpack");
            ArrayList<User> list = new ArrayList<User>();
            list.add(user);
            System.out.println("序列化前: " + list.get(0).getId());

            String a = serializationList(list, User.class, pack);
            List<User> newValue = deserializationList(a, User.class, pack);
            System.out.println("反序列化: " + newValue.get(0).getId());
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println();

        // Map
        try {
            System.out.println("Map使用Msgpack");
            Map<String, User> map = new HashMap<String, User>();
            map.put("user", user);
            System.out.println("序列化前: " + map.get("user"));
            String a = serializationMap(map, User.class, pack);
            Map<String, User> maps = deserializationMap(a, User.class, pack);
            System.out.println("反序列化: " + maps.get("user"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println();
        // ComplexEntity(自定义的Entity，只要包含一些集合的属性，List,Map)
        try {
            System.out.println("ComplexEntity使用Msgpack");
            ComplexEntity entity = new ComplexEntity();
            entity.maps.put("user", user);
            entity.lists.add(user);
            entity.lists.add(user);

            System.out.println("序列化前map: " + entity.maps.get("user"));
            System.out.println("序列化前list: " + entity.lists);
            // 序列化
            byte[] entityBytes = pack.write(entity);
            // 反序列化
            ComplexEntity entitys = pack.read(entityBytes, ComplexEntity.class);
            System.out.println("反序列化map: " + entitys.maps.get("user"));
            System.out.println("反序列化list: " + entitys.lists);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private <T extends Serializable> String serializationObject(T obj, MessagePack msgpack) {
        byte[] b = null;
        try {
            b = msgpack.write(obj);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new String(new Base64().encode(b));
    }

    private <T extends Serializable> T deserializationObject(String obj, Class<T> clazz, MessagePack msgpack) {
        T t = null;
        byte[] bytes = new Base64().decode(obj.getBytes());
        try {
            t = msgpack.read(bytes, clazz);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return t;
    }

    private static <T extends Serializable> String serializationList(List<T> obj, Class<T> clazz, MessagePack msgpack) {
        byte[] b = null;
        try {
            b = msgpack.write(obj);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new String(new Base64().encode(b));
    }

    private static <T extends Serializable> List<T> deserializationList(String obj, Class<T> clazz, MessagePack msgpack) {
        Template<T> elementTemplate = msgpack.lookup(clazz);
        Template<List<T>> listTmpl = Templates.tList(elementTemplate);

        List<T> t = null;
        byte[] bytes = new Base64().decode(obj.getBytes());
        try {
            t = msgpack.read(bytes, listTmpl);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return t;
    }

    private static <T extends Serializable> String serializationMap(Map<String, T> obj, Class<T> clazz,
                                                                    MessagePack msgpack) {
        byte[] b = null;
        try {
            b = msgpack.write(obj);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new String(new Base64().encode(b));
    }

    private static <T extends Serializable> Map<String, T> deserializationMap(String obj, Class<T> clazz,
                                                                              MessagePack msgpack) {
        Template<T> elementTemplate = msgpack.lookup(clazz);
        Template<Map<String, T>> listTmpl = Templates.tMap(Templates.TString, elementTemplate);

        Map<String, T> t = null;
        byte[] bytes = new Base64().decode(obj.getBytes());
        try {
            t = msgpack.read(bytes, listTmpl);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return t;
    }

    private static <T extends Serializable> String serializationSet(Set<T> obj, Class<T> clazz, MessagePack msgpack) {
        Template<T> elementTemplate = msgpack.lookup(clazz);
        byte[] b = null;
        try {
            b = msgpack.write(obj, new SetTemplate<T>(elementTemplate));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new String(new Base64().encode(b));
    }

    private static <T extends Serializable> Set<T> deserializationSet(String obj, Class<T> clazz, MessagePack msgpack) {
        Template<T> elementTemplate = msgpack.lookup(clazz);
        Set<T> t = null;
        byte[] bytes = new Base64().decode(obj.getBytes());
        try {
            t = msgpack.read(bytes, new SetTemplate<T>(elementTemplate));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return t;
    }

}

@Message
class User implements Serializable {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -5848295770696335660L;
    private int               id;
    private String            name;
    private transient String  password;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public User(int id, String name, String password){
        this.id = id;
        this.name = name;
        this.password = password;
    }

    @Override
    public String toString() {
        return "User [id=" + id + ", name=" + name + ", password=" + password + "]";
    }

    public User(){

    }

}

@Message
class ComplexEntity implements Serializable {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -458914520933183052L;
    // 为了能直接使用，全部都改为public了
    public List<User>         lists            = new ArrayList<User>();
    public Map<String, User>  maps             = new HashMap<String, User>();

    public Map<String, User> getMaps() {
        return maps;
    }

    public void setMaps(Map<String, User> maps) {
        this.maps = maps;
    }

    public List<User> getLists() {
        return lists;
    }

    public void setLists(List<User> lists) {
        this.lists = lists;
    }
}
