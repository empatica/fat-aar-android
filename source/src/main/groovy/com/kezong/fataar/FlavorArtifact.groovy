package com.kezong.fataar

import com.android.build.gradle.api.LibraryVariant
import com.android.builder.model.ProductFlavor
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier
import org.gradle.api.internal.artifacts.DefaultResolvedArtifact
import org.gradle.api.internal.artifacts.dsl.LazyPublishArtifact
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.tasks.TaskDependencyFactory
import org.gradle.api.tasks.TaskProvider
import org.gradle.internal.Describables
import org.gradle.internal.component.local.model.PublishArtifactLocalArtifactMetadata
import org.gradle.internal.component.model.DefaultIvyArtifactName
import org.gradle.internal.model.CalculatedValueContainerFactory
/**
 * FlavorArtifact
 */
class FlavorArtifact {


    static ResolvedArtifact createFlavorArtifact(Project project,
                                                 LibraryVariant variant,
                                                 ResolvedDependency unResolvedArtifact,
                                                 CalculatedValueContainerFactory calculatedValueContainerFactory,
                                                 FileResolver fileResolver,
                                                 TaskDependencyFactory taskDependencyFactory
    ) {
        Project artifactProject = getArtifactProject(project, unResolvedArtifact)
        TaskProvider bundleProvider = null;
        try {
            bundleProvider = getBundleTask(artifactProject, variant)
        } catch (Exception ex) {
            FatUtils.logError("[$variant.name]Can not resolve :$unResolvedArtifact.moduleName", ex)
            return null
        }

        if (bundleProvider == null) {
            FatUtils.logError("[$variant.name]Can not resolve :$unResolvedArtifact.moduleName")
            return null
        }

        ModuleVersionIdentifier identifier = createModuleVersionIdentifier(unResolvedArtifact)
        File artifactFile = createArtifactFile(bundleProvider.get())
        DefaultIvyArtifactName artifactName = createArtifactName(artifactFile)

        return new DefaultResolvedArtifact(
                new PublishArtifactLocalArtifactMetadata(
                        new ComponentIdentifier() {
                            @Override
                            String getDisplayName() {
                                return artifactName.name
                            }
                        },
                        new LazyPublishArtifact(bundleProvider, fileResolver, taskDependencyFactory)
                ),
                calculatedValueContainerFactory.create(Describables.of(artifactFile.name), artifactFile),
                identifier, artifactName

        )
    }

    private static ModuleVersionIdentifier createModuleVersionIdentifier(ResolvedDependency unResolvedArtifact) {
        return DefaultModuleVersionIdentifier.newId(
                unResolvedArtifact.getModuleGroup(),
                unResolvedArtifact.getModuleName(),
                unResolvedArtifact.getModuleVersion()
        )
    }

    private static DefaultIvyArtifactName createArtifactName(File artifactFile) {
        return new DefaultIvyArtifactName(artifactFile.getName(), "aar", "")
    }


    private static Project getArtifactProject(Project project, ResolvedDependency unResolvedArtifact) {
        for (Project p : project.getRootProject().getAllprojects()) {
            if (unResolvedArtifact.moduleName == p.name && unResolvedArtifact.moduleGroup == p.group.toString()) {
                return p
            }
        }
        return null
    }

    private static File createArtifactFile(Task bundle) {
        return new File(bundle.getDestinationDirectory().getAsFile().get(), bundle.getArchiveFileName().get())
    }

    private static TaskProvider getBundleTask(Project project, LibraryVariant variant) {
        TaskProvider bundleTaskProvider = null
        project.android.libraryVariants.find { subVariant ->
            // 1. find same flavor
            if (variant.name == subVariant.name) {
                try {
                    bundleTaskProvider = VersionAdapter.getBundleTaskProvider(project, subVariant.name as String)
                    return true
                } catch (Exception ignore) {
                }
            }

            // 2. find buildType
            ProductFlavor flavor = variant.productFlavors.isEmpty() ? variant.mergedFlavor : variant.productFlavors.first()
            if (subVariant.name == variant.buildType.name) {
                try {
                    bundleTaskProvider = VersionAdapter.getBundleTaskProvider(project, subVariant.name as String)
                    return true
                } catch (Exception ignore) {
                }
            }

            // 3. find missingStrategies
            try {
                flavor.missingDimensionStrategies.find { entry ->
                    String toDimension = entry.getKey()
                    List<String> toFlavors = [entry.getValue().requested] + entry.getValue().getFallbacks()
                    ProductFlavor subFlavor = subVariant.productFlavors.isEmpty() ?
                            subVariant.mergedFlavor : subVariant.productFlavors.first()
                    toFlavors.find { toFlavor ->
                        if (toDimension == subFlavor.dimension
                                && toFlavor == subFlavor.name
                                && variant.buildType.name == subVariant.buildType.name) {
                            try {
                                bundleTaskProvider = VersionAdapter.getBundleTaskProvider(project, subVariant.name as String)
                                return true
                            } catch (Exception ignore) {
                            }
                        }
                    }
                }
            } catch (Exception ignore) {

            }

            return bundleTaskProvider != null
        }

        return bundleTaskProvider
    }

}
