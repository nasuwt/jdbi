/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jdbi.doc;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.rule.PgDatabaseRule;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class GeneratedKeysTest {
    @Rule
    public PgDatabaseRule dbRule = new PgDatabaseRule()
        .withPlugin(new SqlObjectPlugin())
        .withPlugin(new PostgresPlugin());
    private Jdbi db;

    @Before
    public void getHandle() {
        db = dbRule.getJdbi();
    }

    // tag::setup[]
    public static class User {
        final int id;
        final String name;

        public User(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    @Before
    public void setUp() throws Exception {
        db.useHandle(h -> h.execute("CREATE TABLE users (id SERIAL PRIMARY KEY, name VARCHAR)"));
        db.registerRowMapper(ConstructorMapper.of(User.class));
    }
    // end::setup[]

    @Test
    // tag::fluent[]
    public void fluentInsertKeys() {
        db.useHandle(handle -> {
            User data = handle.createUpdate("INSERT INTO users (name) VALUES(?)")
                    .bind(0, "Data")
                    .executeAndReturnGeneratedKeys()
                    .mapTo(User.class)
                    .findOnly();

            assertEquals(1, data.id); // This value is generated by the database
            assertEquals("Data", data.name);
        });
    }
    // end::fluent[]

    @Test
    // tag::sqlObject[]
    public void sqlObjectBatchKeys() {
        db.useExtension(UserDao.class, dao -> {
            List<User> users = dao.createUsers("Alice", "Bob", "Charlie");
            assertEquals(3, users.size());

            assertEquals(1, users.get(0).id);
            assertEquals("Alice", users.get(0).name);

            assertEquals(2, users.get(1).id);
            assertEquals("Bob", users.get(1).name);

            assertEquals(3, users.get(2).id);
            assertEquals("Charlie", users.get(2).name);
        });
    }

    public interface UserDao {
        @SqlBatch("INSERT INTO users (name) VALUES(?)")
        @GetGeneratedKeys
        List<User> createUsers(String... names);
    }
    // end::sqlObject[]
}
