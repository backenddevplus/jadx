package jadx.tests.integration;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestArgInline extends IntegrationTest {

	public static class TestCls {

		public void method(int a) {
			while (a < 10) {
				int b = a + 1;
				a = b;
			}
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("a++;"));
		assertThat(code, not(containsString("a = a + 1;")));
	}
}
