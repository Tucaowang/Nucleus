package uk.co.drnaylor.minecraft.quickstart.config;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.SimpleConfigurationNode;
import ninja.leaping.configurate.gson.GsonConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.entity.InvulnerabilityData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;
import uk.co.drnaylor.minecraft.quickstart.api.data.JailData;
import uk.co.drnaylor.minecraft.quickstart.api.data.MuteData;
import uk.co.drnaylor.minecraft.quickstart.api.data.WarpLocation;
import uk.co.drnaylor.minecraft.quickstart.api.data.mail.MailData;
import uk.co.drnaylor.minecraft.quickstart.api.exceptions.NoSuchWorldException;
import uk.co.drnaylor.minecraft.quickstart.config.serialisers.LocationNode;
import uk.co.drnaylor.minecraft.quickstart.internal.PermissionUtil;
import uk.co.drnaylor.minecraft.quickstart.internal.interfaces.InternalQuickStartUser;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class UserConfig extends AbstractConfig<ConfigurationNode, GsonConfigurationLoader> implements InternalQuickStartUser {
    private final User user;
    private MuteData muteData;
    private boolean socialSpy;
    private Instant login;
    private Instant logout;
    private boolean invulnerable;
    private boolean fly;
    private List<MailData> mailDataList;
    private JailData jailData;
    private Location<World> locationOnLogin;
    private boolean jailOffline = false;
    private Map<String, LocationNode> homeData;

    public UserConfig(Path file, User user) throws IOException, ObjectMappingException {
        super(file);
        this.user = user;
    }

    @Override
    public void load() throws IOException, ObjectMappingException {
        node = loader.load();
        if (node.getNode("mute").isVirtual()) {
            muteData = null;
        } else {
            muteData = node.getNode("mute").getValue(TypeToken.of(MuteData.class));
        }

        if (node.getNode("jail").isVirtual()) {
            jailData = null;
        } else {
            jailData = node.getNode("jail").getValue(TypeToken.of(JailData.class));
        }

        jailOffline = node.getNode("jailOnLogin").getBoolean(false);
        socialSpy = node.getNode("socialspy").getBoolean(false);
        login = Instant.ofEpochMilli(node.getNode("timestamp", "login").getLong());
        logout = Instant.ofEpochMilli(node.getNode("timestamp", "logout").getLong());
        invulnerable = node.getNode("invulnerable").getBoolean();
        fly = node.getNode("fly").getBoolean();

        // This returned an immutable list, so we want to make it mutable.
        mailDataList = Lists.newArrayList(node.getNode("mail").getList(TypeToken.of(MailData.class)));

        ConfigurationNode ccn = node.getNode("locationOnLogin");
        if (!ccn.isVirtual()) {
            Optional<World> ow = Sponge.getServer().getWorld(ccn.getNode("world").getValue(TypeToken.of(UUID.class)));
            if (ow.isPresent()) {
                locationOnLogin = new Location<>(ow.get(), node.getNode("x").getDouble(), node.getNode("y").getDouble(), node.getNode("z").getDouble());
            } else {
                WorldProperties wp = Sponge.getServer().getDefaultWorld().get();
                locationOnLogin = new Location<>(Sponge.getServer().getWorld(wp.getUniqueId()).get(), wp.getSpawnPosition());
            }
        }

        // Homes
        if (homeData == null) {
            homeData = Maps.newHashMap();
        }

        homeData.clear();
        node.getNode("homes").getChildrenMap().forEach((k, v) -> homeData.put(k.toString().toLowerCase(), new LocationNode(v)));
    }

    @Override
    public void save() throws IOException, ObjectMappingException {
        if (muteData == null) {
            node.removeChild("mute");
        } else {
            node.getNode("mute").setValue(TypeToken.of(MuteData.class), muteData);
        }

        if (jailData == null) {
            node.removeChild("jail");
        } else {
            node.getNode("jail").setValue(TypeToken.of(JailData.class), jailData);
        }

        node.getNode("socialspy").setValue(isSocialSpy());
        node.getNode("timestamp", "login").setValue(login.toEpochMilli());
        node.getNode("timestamp", "logout").setValue(logout.toEpochMilli());
        node.getNode("invulnerable").setValue(invulnerable);
        node.getNode("fly").setValue(fly);
        node.getNode("jailOnLogin").setValue(jailOffline);

        // Type erasure! Thanks to Google's Type Token, we work around it.
        node.getNode("mail").setValue(new TypeToken<List<MailData>>() {}, mailDataList);

        if (locationOnLogin == null) {
            node.removeChild("locationOnLogin");
        } else {
            ConfigurationNode ccn = node.getNode("locationOnLogin");
            node.getNode("x").setValue(locationOnLogin.getX());
            node.getNode("y").setValue(locationOnLogin.getY());
            node.getNode("z").setValue(locationOnLogin.getZ());
            node.getNode("world").setValue(locationOnLogin.getExtent().getUniqueId());
        }

        homeData.forEach((k, v) -> v.populateNode(node.getNode("homes").getNode(k.toLowerCase())));
        super.save();
    }

    @Override
    protected GsonConfigurationLoader getLoader(Path file) {
        return GsonConfigurationLoader.builder().setPath(file).build();
    }

    @Override
    protected ConfigurationNode getDefaults() {
        return SimpleConfigurationNode.root();
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public Optional<MuteData> getMuteData() {
        return Optional.ofNullable(muteData);
    }

    @Override
    public void setMuteData(MuteData data) {
        this.muteData = data;
    }

    @Override
    public void removeMuteData() {
        this.muteData = null;
    }

    @Override
    public boolean isSocialSpy() {
        socialSpy = socialSpy && (user.hasPermission(PermissionUtil.PERMISSIONS_PREFIX + "socialspy.base") || user.hasPermission(PermissionUtil.PERMISSIONS_ADMIN));
        return socialSpy;
    }

    @Override
    public boolean setSocialSpy(boolean socialSpy) {
        this.socialSpy = socialSpy;

        // Permission checks! Return true if it's what we wanted.
        return isSocialSpy() == socialSpy;
    }

    @Override
    public boolean isInvulnerable() {
        if (user.isOnline()) {
            invulnerable = user.getPlayer().get().get(Keys.INVULNERABILITY_TICKS).orElse(0) > 0;
        }

        return invulnerable;
    }

    @Override
    public boolean setInvulnerable(boolean invuln) {
        if (user.isOnline()) {
            Player pl = user.getPlayer().get();
            Optional<InvulnerabilityData> oid = pl.get(InvulnerabilityData.class);

            if (!oid.isPresent()) {
                return false;
            }

            InvulnerabilityData id = oid.get();
            id.invulnerableTicks().set(invuln ? Integer.MAX_VALUE : 0);
            if (!pl.offer(id).isSuccessful()) {
                return false;
            }
        }

        invulnerable = invuln;
        return true;
    }

    @Override
    public boolean isFlying() {
        if (user.isOnline()) {
            fly = user.getPlayer().get().get(Keys.CAN_FLY).orElse(false);
        }

        return fly;
    }

    @Override
    public boolean setFlying(boolean fly) {
        if (user.isOnline()) {
            Player pl = user.getPlayer().get();
            if (!fly && !pl.offer(Keys.IS_FLYING, false).isSuccessful()) {
                return false;
            }

            if (!pl.offer(Keys.CAN_FLY, fly).isSuccessful()) {
                return false;
            }
        }

        this.fly = fly;
        return true;
    }

    @Override
    public Optional<JailData> getJailData() {
        return Optional.ofNullable(jailData);
    }

    @Override
    public Instant getLastLogin() {
        return login;
    }

    @Override
    public void setLastLogin(Instant login) {
        this.login = login;
    }

    @Override
    public Instant getLastLogout() {
        return logout;
    }

    @Override
    public Optional<WarpLocation> getHome(String home) {
        LocationNode ln = homeData.get(home.toLowerCase());
        if (ln != null) {
            try {
                return Optional.of(new WarpLocation(home.toLowerCase(), ln.getLocation(), ln.getRotation()));
            } catch (NoSuchWorldException e) {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    @Override
    public Map<String, WarpLocation> getHomes() {
        return homeData.entrySet().stream().map(x -> {
            try {
                return new WarpLocation(x.getKey(), x.getValue().getLocation(), x.getValue().getRotation());
            } catch (NoSuchWorldException e) {
                return null;
            }
        }).filter(x -> x != null).collect(Collectors.toMap(WarpLocation::getName, y -> new WarpLocation(y.getName(), y.getLocation(), y.getRotation())));
    }

    @Override
    public boolean setHome(String home, Location<World> location, Vector3d rotation) {
        final Pattern warpName = Pattern.compile("^[a-zA-Z][a-zA-Z0-9]{1,15}$");

        if (homeData.containsKey(home.toLowerCase()) || !warpName.matcher(home).matches()) {
            return false;
        }

        homeData.put(home.toLowerCase(), new LocationNode(location, rotation));
        return true;
    }

    @Override
    public boolean deleteHome(String home) {
        if (homeData.containsKey(home.toLowerCase())) {
            homeData.remove(home.toLowerCase());
            return true;
        }

        return false;
    }

    @Override
    public List<MailData> getMail() {
        return ImmutableList.copyOf(mailDataList);
    }

    @Override
    public void addMail(MailData mailData) {
        mailDataList.add(mailData);
    }

    @Override
    public void clearMail() {
        mailDataList.clear();
    }

    @Override
    public boolean isFlyingSafe() {
        return fly;
    }

    @Override
    public boolean isInvulnerableSafe() {
        return invulnerable;
    }

    @Override
    public void setJailData(JailData data) {
        jailData = data;
    }

    @Override
    public void removeJailData() {
        setJailData(null);
    }

    @Override
    public void setOnLogout() {
        setLastLogout(Instant.now());

        // Set data based toggles.
        isFlying();
        isInvulnerable();
    }

    @Override
    public Optional<Location<World>> getLocationOnLogin() {
        return Optional.ofNullable(locationOnLogin);
    }

    @Override
    public void sendToLocationOnLogin(Location<World> worldLocation) {
        locationOnLogin = worldLocation;
    }

    @Override
    public void setLastLogout(Instant logout) {
        this.logout = logout;
    }

    @Override
    public UUID getUniqueID() {
        return user.getUniqueId();
    }

    @Override
    public boolean jailOnNextLogin() {
        return jailOffline;
    }

    @Override
    public void setJailOnNextLogin(boolean set) {
        jailOffline = !user.isOnline() && set;
    }
}
