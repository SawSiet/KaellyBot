package data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.ClientConfig;
import util.Connexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by steve on 12/11/2016.
 */
public class JobUser {

    private final static Logger LOG = LoggerFactory.getLogger(JobUser.class);
    private String name;
    private int level;
    private User user;

    public JobUser(String name, int level, User user){
        this.name = name;
        if (level > 100) // ToDo : Done change level max for Dofus Touch
            level = 100;
        this.level = level;
        this.user = user;
    }

    public synchronized void setLevel(int level){
        if (level > 100) // ToDo : Done change level max for Dofus Touch
            level = 100;
        this.level = level;

        Connexion connexion = Connexion.getInstance();
        Connection connection = connexion.getConnection();

        try {
            PreparedStatement preparedStatement;
            if (level > 0) {
                preparedStatement = connection.prepareStatement(
                        "UPDATE Job_User SET level = ?"
                                + "WHERE name_job = ? AND id_user = ? AND id_guild = ?;");
                preparedStatement.setInt(1, level);
                preparedStatement.setString(2, name);
                preparedStatement.setString(3, user.getId());
                preparedStatement.setString(4, user.getGuild().getId());
            }
            else {
                user.getJobs().remove(name);
                preparedStatement = connection.prepareStatement(
                        "DELETE FROM Job_User WHERE name_job = ? AND id_user = ? AND id_guild = ?;");
                preparedStatement.setString(1, name);
                preparedStatement.setString(2, user.getId());
                preparedStatement.setString(3, user.getGuild().getId());
            }

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            ClientConfig.setSentryContext(ClientConfig.DISCORD().getGuildByID(Long.parseLong(user.getGuild().getId())),
                    ClientConfig.DISCORD().getUserByID(Long.parseLong(user.getId())), null,null);
            LOG.error(e.getMessage());
        }
    }

    public synchronized void addToDatabase(){
        if (! user.getJobs().containsKey(name) && level > 0) {
            user.getJobs().put(name, this);
            Connexion connexion = Connexion.getInstance();
            Connection connection = connexion.getConnection();

            try {
                PreparedStatement preparedStatement = connection.prepareStatement(
                        "INSERT INTO Job_User(name_job, id_user, id_guild, level) VALUES(?, ?, ?, ?);");
                preparedStatement.setString(1, name);
                preparedStatement.setString(2, user.getId());
                preparedStatement.setString(3, user.getGuild().getId());
                preparedStatement.setInt(4, level);

                preparedStatement.executeUpdate();
            } catch (SQLException e) {
                ClientConfig.setSentryContext(ClientConfig.DISCORD().getGuildByID(Long.parseLong(user.getGuild().getId())),
                        ClientConfig.DISCORD().getUserByID(Long.parseLong(user.getId())), null,null);
                LOG.error(e.getMessage());
            }
        }
    }

    public synchronized static Map<String, JobUser> getJobUsers(User user){
        Map<String, JobUser> jobs = new ConcurrentHashMap<>();

        Connexion connexion = Connexion.getInstance();
        Connection connection = connexion.getConnection();

        String name;
        int level;

        try {
            PreparedStatement query = connection.prepareStatement("SELECT name_job, level"
                    + " FROM Job_User WHERE id_user = ? AND id_guild = ?;");
            query.setString(1, user.getId());
            query.setString(2, user.getGuild().getId());

            ResultSet resultSet = query.executeQuery();

            while (resultSet.next()) {
                name = resultSet.getString("name_job");
                level = resultSet.getInt("level");
                jobs.put(name, new JobUser(name, level, user));
            }
        } catch (SQLException e) {
            ClientConfig.setSentryContext(ClientConfig.DISCORD().getGuildByID(Long.parseLong(user.getGuild().getId())),
                    ClientConfig.DISCORD().getUserByID(Long.parseLong(user.getId())), null,null);
            LOG.error(e.getMessage());
        }

        return jobs;
    }

    public String getName() {
        return name;
    }

    public int getLevel(){
        return level;
    }
}
