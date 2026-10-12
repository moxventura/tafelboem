package nl.jeeninga.tafelboem.build;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

/**
 * Builds {@code TafelBoem.zip}, the Prism instance new PCs import; see {@link PrismInstance}.
 */
public abstract class PrismInstanceZip extends DefaultTask {
	@Input
	public abstract Property<String> getInstanceName();

	@Input
	public abstract Property<String> getMinecraft();

	@Input
	public abstract Property<String> getFabric();

	@Input
	public abstract Property<String> getPackUrl();

	@InputFile
	public abstract RegularFileProperty getBootstrap();

	@InputFile
	public abstract RegularFileProperty getInstaller();

	@OutputFile
	public abstract RegularFileProperty getZip();

	@TaskAction
	public void build() {
		PrismInstance.writeZip(getZip().get().getAsFile().toPath(), getInstanceName().get(), getMinecraft().get(), getFabric().get(),
				getPackUrl().get(), getBootstrap().get().getAsFile().toPath(), getInstaller().get().getAsFile().toPath());
	}
}
