package com.mcon152.recipeshare.service;

import com.mcon152.recipeshare.Recipe;
import com.mcon152.recipeshare.repository.RecipeRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Assignment: Implement all TODOs using Mockito features covered in class:
 * - @Mock, @InjectMocks, @Captor, @ExtendWith(MockitoExtension.class)
 * - Stubbing: thenReturn / thenAnswer / thenThrow
 * - Verifications: verify(...), times/never/atLeast..., verifyNoMoreInteractions
 * - InOrder (where meaningful)
 * - Void stubbing: doNothing / doThrow (use deleteById for this)
 * - Matchers: any(), eq(), argThat()
 * - ArgumentCaptor
 * - (Optional) Spy demo if you introduce a small helper in tests
 * <p>
 * NOTE: This is a pure unit test. Do NOT start a Spring context.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RecipeService (Mockito) — Assignment Skeleton")
class RecipeServiceTest {

    @Mock
    private RecipeRepository recipeRepository;

    @InjectMocks
    private RecipeServiceImpl recipeService; // CUT implements RecipeService

    @Captor
    private ArgumentCaptor<Recipe> recipeCaptor;

    // --- Helpers for sample data ---

    private Recipe newRecipeNoId() {
        return new Recipe(
                null,
                "Chocolate Cake",
                "Moist chocolate cake",
                "flour, eggs, cocoa",
                "mix, bake",
                8
        );
    }

    private Recipe savedRecipe(long id) {
        return new Recipe(
                id,
                "Chocolate Cake",
                "Moist chocolate cake",
                "flour, eggs, cocoa",
                "mix, bake",
                8
        );
    }

    // ------------------ addRecipe ------------------

    @Nested
    @DisplayName("addRecipe(Recipe)")
    class AddRecipe {

        @Test
        @DisplayName("returns saved entity (thenReturn) and calls repository.save once")
        void returnsSaved_andSavesOnce() {
            // 1) when(recipeRepository.save(...)).thenReturn(savedRecipe(1L))
            // 2) call recipeService.addRecipe(newRecipeNoId())
            // 3) assert non-null id and fields
            // 4) verify(recipeRepository).save(any(Recipe.class)); verifyNoMoreInteractions(recipeRepository)

            //See code below as an example answer

            Recipe input = newRecipeNoId();
            Recipe saved = savedRecipe(1L);

            when(recipeRepository.save(any(Recipe.class))).thenReturn(saved);

            Recipe out = recipeService.addRecipe(input);
            assertNotNull(out, "service should return a non-null recipe");
            assertEquals(1L, out.getId(), "saved recipe should have generated id");
            assertEquals(saved, out, "service should return exactly what repository returns");
            assertEquals("Chocolate Chip Cookies", out.getTitle());
            assertEquals("Classic chewy cookies", out.getDescription());


            verify(recipeRepository).save(any(Recipe.class));
            verifyNoMoreInteractions(recipeRepository);
        }

        @Test
        @DisplayName("assigns ID dynamically (thenAnswer) and captures argument")
        void assignsId_thenAnswer_andCaptures() {
            // 1) Use thenAnswer to return a new Recipe with id=1L, copying fields from arg
            // 2) capture the arg with ArgumentCaptor and assert title, id==null pre-save

            //See code below as an example answer

            when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> {
                Recipe r = inv.getArgument(0);
                return new Recipe(1L, r.getTitle(), r.getDescription(),
                        r.getIngredients(), r.getInstructions(), r.getServings());
            });

            Recipe out = recipeService.addRecipe(newRecipeNoId());
            assertNotNull(out);
            assertEquals(1L, out.getId());

            verify(recipeRepository).save(recipeCaptor.capture());
            verifyNoMoreInteractions(recipeRepository);

            Recipe sent = recipeCaptor.getValue();
            assertNotNull(sent, "captured recipe should not be null");
            assertNull(sent.getId(), "pre-save ID should be null");
            assertEquals("Chocolate Cake", sent.getTitle(), "title should match input");
            assertEquals("Rich and moist cake", sent.getDescription());
            assertEquals("Flour, sugar, cocoa, eggs, butter", sent.getIngredients());
            assertEquals("Mix, bake at 350°F for 30 min", sent.getInstructions());
            assertEquals(8, sent.getServings());

        }

        @Test
        @DisplayName("propagates repository failure (thenThrow)")
        void propagatesRepositoryFailure() {
            // when(recipeRepository.save(any())).thenThrow(new IllegalStateException("DB down"))
            // assertThrows on recipeService.addRecipe(...)
            when(recipeRepository.save(any(Recipe.class)))
                    .thenThrow(new IllegalStateException("DB down"));

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> recipeService.addRecipe(newRecipeNoId()),
                    "addRecipe should propagate repository exception");
            assertEquals("DB down", ex.getMessage());

            verify(recipeRepository, times(1)).save(any(Recipe.class));
            verifyNoMoreInteractions(recipeRepository);
        }
    }

    // ------------------ getAllRecipes ------------------

    @Nested
    @DisplayName("getAllRecipes()")
    class GetAllRecipes {

        @Test
        @DisplayName("returns list from repository")
        void returnsList() {
            // when(recipeRepository.findAll()).thenReturn(List.of(...))
            // assert same size/content; verify(findAll)

            Recipe r1 = savedRecipe(1L);
            r1.setTitle("Chocolate Cake");
            Recipe r2 = savedRecipe(2L);
            r2.setTitle("Banana Bread");

            when(recipeRepository.findAll()).thenReturn(List.of(r1, r2));

            List<Recipe> out = recipeService.getAllRecipes();

            assertEquals(2, out.size(), "list size should match repository return");
            assertEquals(List.of(r1, r2), out, "service should return exactly what repository returns");
            assertEquals("Chocolate Cake", out.get(0).getTitle());
            assertEquals("Banana Bread", out.get(1).getTitle());
        }
    }

    // ------------------ getRecipeById ------------------

    @Nested
    @DisplayName("getRecipeById(long)")
    class GetById {

        @Test
        @DisplayName("returns Optional.present when found")
        void present() {
            long id = 1L;
            Recipe found = savedRecipe(id);
            when(recipeRepository.findById(id)).thenReturn(Optional.of(found));

            Optional<Recipe> out = recipeService.getRecipeById(id);

            assertNotNull(out, "service should return a non-null Optional");
            assertTrue(out.isPresent(), "Optional should be present when repository returns a value");
            assertEquals(found, out.get(), "content should equal repository result");

            verify(recipeRepository, times(1)).findById(id);
            verifyNoMoreInteractions(recipeRepository);
        }

        @Test
        @DisplayName("returns Optional.empty when missing")
        void empty() {
            long id = 42L;
            when(recipeRepository.findById(id)).thenReturn(Optional.empty());

            Optional<Recipe> out = recipeService.getRecipeById(id);

            assertNotNull(out, "service should return a non-null Optional");
            assertTrue(out.isEmpty(), "Optional should be empty when repository returns empty");

            verify(recipeRepository, times(1)).findById(id);
            verifyNoMoreInteractions(recipeRepository);
        }
    }

    // ------------------ deleteRecipe ------------------

    @Nested
    @DisplayName("deleteRecipe(long)")
    class DeleteRecipe {

        @Test
        @DisplayName("returns true when entity existed")
        void returnsTrue_whenExists() {
            // when(recipeRepository.existsById(id)).thenReturn(true)
            // doNothing().when(recipeRepository).deleteById(id)
            // assert true; verify order: existsById -> deleteById
            long id = 1L;
            when(recipeRepository.existsById(id)).thenReturn(true);
            doNothing().when(recipeRepository).deleteById(id);

            boolean result = recipeService.deleteRecipe(id);

            assertTrue(result, "service should return true when entity exists and delete succeeds");

            InOrder inOrder = inOrder(recipeRepository);
            inOrder.verify(recipeRepository).existsById(id);
            inOrder.verify(recipeRepository).deleteById(id);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("returns false when missing (never deletes)")
        void returnsFalse_whenMissing() {

            long id = 42L;
            when(recipeRepository.existsById(id)).thenReturn(false);

            boolean result = recipeService.deleteRecipe(id);

            assertFalse(result, "service should return false when entity does not exist");

            verify(recipeRepository, times(1)).existsById(id);
            verify(recipeRepository, never()).deleteById(anyLong());
            verifyNoMoreInteractions(recipeRepository);
        }

        @Test
        @DisplayName("propagates delete error (doThrow)")
        void propagatesDeleteError() {

            long id = 7L;
            when(recipeRepository.existsById(id)).thenReturn(true);
            doThrow(new IllegalStateException("Delete failed")).when(recipeRepository).deleteById(id);

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> recipeService.deleteRecipe(id), "deleteRecipe should propagate repository delete exception");
            assertEquals("Delete failed", ex.getMessage());

            InOrder inOrder = inOrder(recipeRepository);
            inOrder.verify(recipeRepository).existsById(id);
            inOrder.verify(recipeRepository).deleteById(id);
            inOrder.verifyNoMoreInteractions();
        }
    }

    // ------------------ updateRecipe ------------------

    @Nested
    @DisplayName("updateRecipe(long, Recipe)")
    class UpdateRecipe {

        @Test
        @DisplayName("returns updated entity when exists")
        void returnsUpdated_whenExists() {
            // findById -> present(existing)
            // save(...) -> updatedSaved
            // assert Optional.present & fields updated
            // capture arg and assert values
            long id = 1L;
            Recipe existing = existingRecipe(id); // original state in DB
            Recipe patch = patchRecipe();         // incoming changes from client
            Recipe updatedSaved = updatedSaved(id, patch); // what repo returns after save

            when(recipeRepository.findById(id)).thenReturn(Optional.of(existing));
            when(recipeRepository.save(any(Recipe.class))).thenReturn(updatedSaved);

            Optional<Recipe> outOpt = recipeService.updateRecipe(id, patch);

            assertNotNull(outOpt, "service should not return null Optional");
            assertTrue(outOpt.isPresent(), "Optional should be present when entity exists");
            Recipe out = outOpt.get();
            assertEquals(id, out.getId());
            assertEquals(patch.getTitle(), out.getTitle());
            assertEquals(patch.getDescription(), out.getDescription());
            assertEquals(patch.getIngredients(), out.getIngredients());
            assertEquals(patch.getInstructions(), out.getInstructions());
            assertEquals(patch.getServings(), out.getServings());

            verify(recipeRepository).findById(id);
            verify(recipeRepository).save(recipeCaptor.capture());
            verifyNoMoreInteractions(recipeRepository);

            Recipe toSave = recipeCaptor.getValue();
            assertNotNull(toSave, "captured entity should not be null");
            assertEquals(id, toSave.getId(), "service should save entity with same ID");
            assertEquals(patch.getTitle(), toSave.getTitle());
            assertEquals(patch.getDescription(), toSave.getDescription());
            assertEquals(patch.getIngredients(), toSave.getIngredients());
            assertEquals(patch.getInstructions(), toSave.getInstructions());
            assertEquals(patch.getServings(), toSave.getServings());
        }

        @Test
        @DisplayName("returns empty when entity missing")
        void returnsEmpty_whenMissing() {
            long id = 99L;
            Recipe patch = patchRecipe();
            when(recipeRepository.findById(id)).thenReturn(Optional.empty());

            // Act
            Optional<Recipe> outOpt = recipeService.updateRecipe(id, patch);

            // Assert
            assertNotNull(outOpt, "service should return a non-null Optional");
            assertTrue(outOpt.isEmpty(), "Optional should be empty when entity is missing");

            // Verify: findById called, save never called
            verify(recipeRepository, times(1)).findById(id);
            verify(recipeRepository, never()).save(any(Recipe.class));
            verifyNoMoreInteractions(recipeRepository);
        }
    }

    private Recipe existingRecipe(long id) {
        Recipe r = new Recipe();
        r.setId(id);
        r.setTitle("Chocolate Cake");
        r.setDescription("Rich and moist cake");
        r.setIngredients("Flour, sugar, cocoa, eggs, butter");
        r.setInstructions("Mix, bake at 350°F for 30 min");
        r.setServings(8);
        return r;
    }

    private Recipe patchRecipe() {
        Recipe r = new Recipe();
        // Typically patch payload has no ID or same ID; service should preserve path ID.
        r.setTitle("Ultimate Chocolate Cake");
        r.setDescription("Even richer and moister");
        r.setIngredients("Flour, sugar, dark cocoa, eggs, butter, vanilla");
        r.setInstructions("Sift dry, whisk wet, combine, bake at 350°F for 32 min");
        r.setServings(10);
        return r;
    }

    private Recipe updatedSaved(long id, Recipe patch) {
        Recipe r = new Recipe();
        r.setId(id);
        r.setTitle(patch.getTitle());
        r.setDescription(patch.getDescription());
        r.setIngredients(patch.getIngredients());
        r.setInstructions(patch.getInstructions());
        r.setServings(patch.getServings());
        return r;
    }


    // ------------------ patchRecipe ------------------

    @Nested
    @DisplayName("patchRecipe(long, Recipe)")
    class PatchRecipe {

        @Test
        @DisplayName("applies only non-null fields (argThat)")
        void appliesNonNullFields_only() {
            // findById -> present(existing)
            // provide partial with only title set
            // repository.save returns the modified entity (use thenAnswer echo)
            // verify save(argThat(...)) to ensure unchanged fields remain as-is
            long id = 5L;
            Recipe existing = existingRecipe(id);

            Recipe patch = new Recipe();
            patch.setTitle("Ultimate Chocolate Cake");

            when(recipeRepository.findById(id)).thenReturn(Optional.of(existing));

            when(recipeRepository.save(any(Recipe.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Optional<Recipe> outOpt = recipeService.patchRecipe(id, patch);

            assertNotNull(outOpt, "service should return a non-null Optional");
            assertTrue(outOpt.isPresent(), "should be present when entity exists");
            Recipe out = outOpt.get();
            assertEquals(id, out.getId(), "id should be preserved");
            assertEquals("Ultimate Chocolate Cake", out.getTitle(), "title must be updated");
            assertEquals(existing.getDescription(), out.getDescription(), "description should remain unchanged");
            assertEquals(existing.getIngredients(), out.getIngredients(), "ingredients should remain unchanged");

            assertEquals(existing.getInstructions(), out.getInstructions(), "instructions should remain unchanged");
            assertEquals(existing.getServings(), out.getServings(), "servings should remain unchanged");

            verify(recipeRepository).findById(id);
            verify(recipeRepository).save(argThat(saved -> saved.getId() != null
                    && saved.getId().equals(id)
                    && "Ultimate Chocolate Cake".equals(saved.getTitle())
                    && existing.getDescription().equals(saved.getDescription())
                    && existing.getIngredients().equals(saved.getIngredients())
                    && existing.getInstructions().equals(saved.getInstructions())
                    && existing.getServings() == saved.getServings()
            ));
            verifyNoMoreInteractions(recipeRepository);


        }

        @Test
        @DisplayName("returns empty when entity missing")
        void returnsEmpty_whenMissing() {

            long id = 404L;
            Recipe patch = new Recipe();
            patch.setTitle("New Title");

            when(recipeRepository.findById(id)).thenReturn(Optional.empty());

            Optional<Recipe> outOpt = recipeService.patchRecipe(id, patch);

            assertNotNull(outOpt, "service should return a non-null Optional");
            assertTrue(outOpt.isEmpty(), "Optional should be empty when entity is missing");
            verify(recipeRepository, times(1)).findById(id);
            verify(recipeRepository, never()).save(any(Recipe.class));
            verifyNoMoreInteractions(recipeRepository);
        }
    }

    // ------------------ extra practice ------------------

    @Nested
    @DisplayName("Advanced stubbing & verification")
    class Advanced {

        @Test
        @DisplayName("consecutive stubs on existsById (true, false)")
        void consecutiveStubs_existsById() {

            long id = 1L;
            when(recipeRepository.existsById(id)).thenReturn(true, false);

            doNothing().when(recipeRepository).deleteById(id);

            boolean first = recipeService.deleteRecipe(id);
            boolean second = recipeService.deleteRecipe(id);

            assertTrue(first, "First delete should return true when entity exists");
            assertFalse(second, "Second delete should return false when entity no longer exists");

            verify(recipeRepository, times(2)).existsById(id);
            verify(recipeRepository, times(1)).deleteById(id);
            verifyNoMoreInteractions(recipeRepository);


        }
    }
}
