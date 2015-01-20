package test.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.eclipse.persistence.config.EntityManagerProperties;
import org.eclipse.persistence.config.PersistenceUnitProperties;

import test.Entity0;
import test.Entity17;
import test.Entity3;

/**
 * Hello world!
 *
 */
public class App {
	public static void main(String[] args) {
		ArrayList<Long> times = new ArrayList<>();
		Map<String, Object> properties = new HashMap<>();
		FilteringClassLoader cl = new FilteringClassLoader("test", Thread.currentThread().getContextClassLoader());
		properties.put(PersistenceUnitProperties.CLASSLOADER, cl);
		System.out.println("Generating Schema");
		{
			Map<String, Object> generateProperties = new HashMap<>(properties);
			// PersistenceUnitProperties.SCHEMA_GENERATION_DATABASE_ACTION
			//generateProperties.put(
			//		"javax.persistence.schema-generation.database.action",
			//		"create");
			EntityManagerFactory emf = Persistence.createEntityManagerFactory(
					"my-app", generateProperties);

			emf.close();
		}
		for (int i = 0; i < 3; i++) {
			long start = System.currentTimeMillis();
			try {
				System.out.println("Loading");
				EntityManagerFactory emf = Persistence
						.createEntityManagerFactory("my-app", properties);
				EntityManager em = emf.createEntityManager();
				em.getTransaction().begin();
				em.find(Entity0.class, 0);

				Entity0 en = new Entity0();
				Entity17 en2 = new Entity17();
			//	en.setManyToOne83(en2);
				em.persist(en);
				em.persist(en2);

				em.getTransaction().commit();
				emf.close();
			} catch (Throwable t) {
				t.printStackTrace();
			}
			times.add(System.currentTimeMillis() - start);
		}

		for (long time : times) {
			System.out.println("Time: " + time);
		}

		System.exit(0);
	}
}
