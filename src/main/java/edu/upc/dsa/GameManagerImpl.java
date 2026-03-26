package edu.upc.dsa;

import edu.upc.dsa.exceptions.*;
import edu.upc.dsa.models.User;
import edu.upc.dsa.models.GameObject;
import edu.upc.dsa.models.UserGameObject;
import edu.upc.dsa.models.Evento;
import edu.upc.dsa.models.Objects;
import edu.upc.dsa.util.ValidationUtils;
import org.mindrot.jbcrypt.BCrypt;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Single implementation of GameManager that works in-memory.
 */
public class GameManagerImpl implements GameManager {

    private static class SingletonHolder {
        private static final GameManager INSTANCE = new GameManagerImpl();
    }

    private final Map<String, User> users;
    private final Map<String, GameObject> objectsByName;
    private final Map<String, GameObject> objectsById;
    private final Map<String, List<UserGameObject>> userInventory;
    private final List<Evento> eventos;
    
    final static Logger logger = Logger.getLogger(GameManagerImpl.class);

    protected GameManagerImpl() {
        System.out.println("[DEBUG] GameManagerImpl constructor started");
        this.users = new ConcurrentHashMap<>();
        this.objectsByName = new ConcurrentHashMap<>();
        this.objectsById = new ConcurrentHashMap<>();
        this.userInventory = new ConcurrentHashMap<>();
        this.eventos = new LinkedList<>();

        try {
            this.eventos.add(new Evento("1", "Evento 1", "Torneo individual", "16-12-2025", "20-12-2025", ""));
            this.eventos.add(new Evento("2", "Evento 2", "Torneo por equipos", "16-12-2025", "20-12-2025", ""));
            
            // Initial objects - Use specific enum values
            System.out.println("[DEBUG] Adding initial objects...");
            addNewObjeto("Espada", "Corta dragones", Objects.ESPADA, 10);
            addNewObjeto("Escudo", "Resistente al fuego", Objects.ESCUDO, 20);
            addNewObjeto("Pocion", "Recupera energía", Objects.POCION, 30);
            System.out.println("[DEBUG] Initial objects added.");
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to initialize GameManagerImpl: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static GameManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    @Override
    public void clear() {
        users.clear();
        objectsByName.clear();
        objectsById.clear();
        userInventory.clear();
    }

    @Override
    public User Register(String username, String password, String email) throws UserAlreadyExistsException {
        ValidationUtils.validateUsername(username);
        ValidationUtils.validatePassword(password);
        ValidationUtils.validateEmail(email);

        username = username.toLowerCase();
        email = email.toLowerCase();

        if (users.containsKey(username)) {
            throw new UserAlreadyExistsException("El usuario '" + username + "' ya existe");
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));
        User u = new User(username, hashedPassword, email);
        users.put(username, u);
        userInventory.put(username, new ArrayList<>());
        logger.info("Usuario registrado en memoria: " + username);
        return u;
    }

    @Override
    public User LogIn(String username, String password) throws FailedLoginException {
        ValidationUtils.validateNotEmpty(username, "username");
        ValidationUtils.validateNotEmpty(password, "password");

        username = username.toLowerCase();
        User u = users.get(username);
        if (u == null || !BCrypt.checkpw(password, u.getPassword())) {
            throw new FailedLoginException("Usuario o contraseña incorrectas");
        }
        return u;
    }

    @Override
    public GameObject addNewObjeto(String nombre, String descripcion, Objects tipo, int precio) {
        ValidationUtils.validateNotEmpty(nombre, "nombre");
        ValidationUtils.validatePositive(precio, "precio");

        if (objectsByName.containsKey(nombre)) {
            return objectsByName.get(nombre);
        }

        GameObject o = new GameObject(nombre, descripcion, tipo, precio);
        objectsByName.put(nombre, o);
        objectsById.put(o.getId(), o);
        return o;
    }

    @Override
    public String getObjectId(String objectName) throws ObjectNotFoundException {
        ValidationUtils.validateNotEmpty(objectName, "objectName");
        GameObject o = objectsByName.get(objectName);
        if (o == null) {
            throw new ObjectNotFoundException("Objeto no encontrado: " + objectName);
        }
        return o.getId();
    }

    @Override
    public List<UserGameObject> getListObjects(String username) throws UserNotFoundException {
        ValidationUtils.validateNotEmpty(username, "username");
        username = username.toLowerCase();
        if (!users.containsKey(username)) {
            throw new UserNotFoundException("Usuario no encontrado: " + username);
        }
        List<UserGameObject> inv = userInventory.get(username);
        return inv != null ? inv : new ArrayList<>();
    }

    @Override
    public User addObjectToUser(String username, String objectId)
            throws UserNotFoundException, ObjectNotFoundException {
        ValidationUtils.validateNotEmpty(username, "username");
        ValidationUtils.validateNotEmpty(objectId, "objectId");

        username = username.toLowerCase();
        User u = users.get(username);
        if (u == null) throw new UserNotFoundException("Usuario no encontrado: " + username);

        GameObject o = objectsById.get(objectId);
        if (o == null) throw new ObjectNotFoundException("Objeto no encontrado: " + objectId);

        userInventory.get(username).add(new UserGameObject(o, 1));
        return u;
    }

    @Override
    public User purchaseObject(String username, String objectId)
            throws UserNotFoundException, ObjectNotFoundException, InsufficientFundsException {
        ValidationUtils.validateNotEmpty(username, "username");
        ValidationUtils.validateNotEmpty(objectId, "objectId");

        username = username.toLowerCase();
        User u = users.get(username);
        if (u == null) throw new UserNotFoundException("Usuario no encontrado: " + username);

        GameObject o = objectsById.get(objectId);
        if (o == null) throw new ObjectNotFoundException("Objeto no encontrado: " + objectId);

        if (u.getMonedas() < o.getPrecio()) throw new InsufficientFundsException("Saldo insuficiente");

        u.setMonedas(u.getMonedas() - o.getPrecio());
        userInventory.get(username).add(new UserGameObject(o, 1));
        return u;
    }

    @Override
    public User getUser(String username) {
        if (username == null) return null;
        return users.get(username.toLowerCase());
    }

    @Override
    public List<User> getUsers() {
        return new ArrayList<>(users.values());
    }

    @Override
    public int getNumberOfUsersRegistered() {
        return users.size();
    }

    @Override
    public List<GameObject> getAllStoreObjects() {
        return new ArrayList<>(objectsByName.values());
    }

    @Override
    public List<Evento> getEventos() {
        return eventos;
    }

    @Override
    public boolean registerEvento(String userId, String eventoId) {
        return userId != null && eventoId != null;
    }

    @Override
    public void addCoinsToUser(String username, int amount) throws UserNotFoundException {
        ValidationUtils.validateNotEmpty(username, "username");
        username = username.toLowerCase();
        User u = users.get(username);
        if (u == null) throw new UserNotFoundException("Usuario no encontrado: " + username);
        u.setMonedas(u.getMonedas() + amount);
    }

    @Override
    public void updateUserProgress(String username, Integer actFrag, Integer bestScore) throws UserNotFoundException {
        ValidationUtils.validateNotEmpty(username, "username");
        username = username.toLowerCase();
        User u = users.get(username);
        if (u == null) throw new UserNotFoundException("Usuario no encontrado: " + username);
        if (actFrag != null) u.setActFrag(actFrag);
        if (bestScore != null && bestScore > u.getBestScore()) u.setBestScore(bestScore);
    }

    @Override
    public void updateObjectQuantity(String username, String objectId, int newQuantity)
            throws UserNotFoundException, ObjectNotFoundException {
        ValidationUtils.validateNotEmpty(username, "username");
        ValidationUtils.validateNotEmpty(objectId, "objectId");
        username = username.toLowerCase();
        if (!users.containsKey(username)) throw new UserNotFoundException("Usuario no encontrado: " + username);
        if (!objectsById.containsKey(objectId)) throw new ObjectNotFoundException("Objeto no encontrado: " + objectId);

        List<UserGameObject> inv = userInventory.get(username);
        if (newQuantity <= 0) {
            inv.removeIf(ugo -> ugo.getGameObject().getId().equals(objectId));
        } else {
            for (UserGameObject ugo : inv) {
                if (ugo.getGameObject().getId().equals(objectId)) {
                    ugo.setCantidad(newQuantity);
                    return;
                }
            }
        }
    }
}
