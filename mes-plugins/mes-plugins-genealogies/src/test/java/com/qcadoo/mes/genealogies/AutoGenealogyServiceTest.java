package com.qcadoo.mes.genealogies;

import static org.junit.Assert.assertNotNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import com.qcadoo.mes.api.DataDefinitionService;
import com.qcadoo.mes.api.Entity;
import com.qcadoo.mes.api.SecurityService;
import com.qcadoo.mes.api.TranslationService;
import com.qcadoo.mes.internal.DefaultEntity;
import com.qcadoo.mes.internal.EntityList;
import com.qcadoo.mes.internal.EntityTree;
import com.qcadoo.mes.model.DataDefinition;
import com.qcadoo.mes.model.search.Restriction;
import com.qcadoo.mes.model.search.Restrictions;
import com.qcadoo.mes.view.ComponentState;
import com.qcadoo.mes.view.ComponentState.MessageType;
import com.qcadoo.mes.view.ViewDefinitionState;
import com.qcadoo.mes.view.components.form.FormComponentState;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ FormComponentState.class, GenealogyService.class, TransactionAspectSupport.class })
public class AutoGenealogyServiceTest {

    private AutoGenealogyService autoGenealogyService;

    private DataDefinitionService dataDefinitionService;

    private TranslationService translationService;

    private GenealogyService genealogyService;

    private SecurityService securityService;

    @Before
    public void init() {
        dataDefinitionService = mock(DataDefinitionService.class, RETURNS_DEEP_STUBS);
        translationService = mock(TranslationService.class);
        genealogyService = mock(GenealogyService.class);
        securityService = mock(SecurityService.class);
        autoGenealogyService = new AutoGenealogyService();
        setField(autoGenealogyService, "dataDefinitionService", dataDefinitionService);
        setField(autoGenealogyService, "translationService", translationService);
        setField(autoGenealogyService, "genealogyService", genealogyService);
        setField(autoGenealogyService, "securityService", securityService);
    }

    @Test
    public void shouldFailAutoCreateGenealogyIfNoRowIsSelected() throws Exception {
        // given
        ComponentState state = mock(ComponentState.class);
        given(state.getFieldValue()).willReturn(null);
        given(state.getLocale()).willReturn(Locale.ENGLISH);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(translationService.translate("core.grid.noRowSelectedError", Locale.ENGLISH)).willReturn(
                "core.grid.noRowSelectedError.pl");

        // when
        autoGenealogyService.autocompleteGenealogy(viewDefinitionState, state, new String[] { "false" });

        // then
        verify(state).addMessage("core.grid.noRowSelectedError.pl", MessageType.FAILURE);
    }

    @Test
    public void shouldFailAutoCreateGenealogyIfFormHasNoIdentifier() throws Exception {
        // given
        FormComponentState state = mock(FormComponentState.class);
        given(state.getFieldValue()).willReturn(null);
        given(state.getLocale()).willReturn(Locale.ENGLISH);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(translationService.translate("core.form.entityWithoutIdentifier", Locale.ENGLISH)).willReturn(
                "core.form.entityWithoutIdentifier.pl");

        // when
        autoGenealogyService.autocompleteGenealogy(viewDefinitionState, state, new String[] { "false" });

        // then
        verify(state).addMessage("core.form.entityWithoutIdentifier.pl", MessageType.FAILURE);
    }

    @Test
    public void shouldFailAutoCreateGenealogyIfOrderIsNull() {
        // given
        ComponentState state = mock(ComponentState.class);
        given(state.getFieldValue()).willReturn(13L);
        given(state.getLocale()).willReturn(Locale.ENGLISH);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);

        given(dataDefinitionService.get("products", "order").get(13L)).willReturn(null);

        given(translationService.translate("core.message.entityNotFound", Locale.ENGLISH)).willReturn(
                "core.message.entityNotFound.pl");
        // when
        autoGenealogyService.autocompleteGenealogy(viewDefinitionState, state, new String[] { "false" });

        // then
        verify(state, times(2)).getFieldValue();
        verify(state).addMessage("core.message.entityNotFound.pl", MessageType.FAILURE);
    }

    @Test
    public void shouldFailAutoCreateGenealogyIfProductIsNull() {
        // given
        ComponentState state = mock(ComponentState.class);
        given(state.getFieldValue()).willReturn(13L);
        given(state.getLocale()).willReturn(Locale.ENGLISH);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);

        Entity order = mock(Entity.class);
        given(order.getBelongsToField("product")).willReturn(null);

        given(dataDefinitionService.get("products", "order").get(13L)).willReturn(order);

        given(translationService.translate("genealogies.message.autoGenealogy.failure.product", Locale.ENGLISH)).willReturn(
                "genealogies.message.autoGenealogy.failure.product.pl");
        // when
        autoGenealogyService.autocompleteGenealogy(viewDefinitionState, state, new String[] { "false" });

        // then
        verify(state, times(2)).getFieldValue();
        verify(state).addMessage("genealogies.message.autoGenealogy.failure.product.pl", MessageType.INFO);
    }

    @Test
    public void shouldFailAutoCreateGenealogyIfTechnologyIsNull() {
        // given
        ComponentState state = mock(ComponentState.class);
        given(state.getFieldValue()).willReturn(13L);
        given(state.getLocale()).willReturn(Locale.ENGLISH);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);

        Entity order = mock(Entity.class);
        Entity product = mock(Entity.class);
        given(order.getBelongsToField("product")).willReturn(product);
        given(order.getBelongsToField("technology")).willReturn(null);

        given(dataDefinitionService.get("products", "order").get(13L)).willReturn(order);

        given(translationService.translate("genealogies.message.autoGenealogy.failure.product", Locale.ENGLISH)).willReturn(
                "genealogies.message.autoGenealogy.failure.product.pl");
        // when
        autoGenealogyService.autocompleteGenealogy(viewDefinitionState, state, new String[] { "false" });

        // then
        verify(state, times(2)).getFieldValue();
        verify(state).addMessage("genealogies.message.autoGenealogy.failure.product.pl", MessageType.INFO);
    }

    @Test
    public void shouldFailAutoCreateGenealogyIfMainBatchIsNull() {
        // given
        ComponentState state = mock(ComponentState.class);
        given(state.getFieldValue()).willReturn(13L);
        given(state.getLocale()).willReturn(Locale.ENGLISH);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);

        Entity order = mock(Entity.class);
        Entity product = mock(Entity.class);
        Entity technology = mock(Entity.class);
        given(order.getBelongsToField("product")).willReturn(product);
        given(order.getBelongsToField("technology")).willReturn(technology);
        given(product.getField("number")).willReturn("test");
        given(product.getField("name")).willReturn("test");

        given(dataDefinitionService.get("products", "order").get(13L)).willReturn(order);

        given(translationService.translate("genealogies.message.autoGenealogy.missingMainBatch", Locale.ENGLISH)).willReturn(
                "genealogies.message.autoGenealogy.missingMainBatch.pl");
        // when
        autoGenealogyService.autocompleteGenealogy(viewDefinitionState, state, new String[] { "false" });

        // then
        verify(state, times(2)).getFieldValue();
        verify(state).addMessage("genealogies.message.autoGenealogy.missingMainBatch.pltest-test", MessageType.INFO, false);
    }

    @Test
    public void shouldFailAutoCreateGenealogyIfExistingGenealogyWithBatch() {
        // given
        ComponentState state = mock(ComponentState.class);
        given(state.getFieldValue()).willReturn(13L);
        given(state.getLocale()).willReturn(Locale.ENGLISH);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);

        Entity order = mock(Entity.class);
        Entity product = mock(Entity.class);
        Entity technology = mock(Entity.class);
        given(order.getBelongsToField("product")).willReturn(product);
        given(order.getBelongsToField("technology")).willReturn(technology);
        given(product.getField("batch")).willReturn("test");

        given(dataDefinitionService.get("products", "order").get(13L)).willReturn(order);

        List<Entity> list = new ArrayList<Entity>();
        list.add(mock(Entity.class));
        given(
                dataDefinitionService.get("genealogies", "genealogy").find().restrictedWith(Restrictions.eq("batch", "test"))
                        .restrictedWith(Restrictions.eq("order.id", order.getId())).withMaxResults(1).list().getEntities())
                .willReturn(list);

        given(translationService.translate("genealogies.message.autoGenealogy.genealogyExist", Locale.ENGLISH)).willReturn(
                "genealogies.message.autoGenealogy.genealogyExist.pl");
        // when
        autoGenealogyService.autocompleteGenealogy(viewDefinitionState, state, new String[] { "false" });

        // then
        verify(state, times(2)).getFieldValue();
        verify(state).addMessage("genealogies.message.autoGenealogy.genealogyExist.pl test", MessageType.INFO);
    }

    @Test
    public void shouldAutoCreateGenealogyWithActualBatch() {
        // given
        ComponentState state = mock(ComponentState.class);
        given(state.getFieldValue()).willReturn(13L);
        given(state.getLocale()).willReturn(Locale.ENGLISH);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        Entity order = mock(Entity.class);
        given(dataDefinitionService.get("products", "order").get(13L)).willReturn(order);
        Entity product = mock(Entity.class);
        Entity technology = mock(Entity.class);
        given(order.getBelongsToField("product")).willReturn(product);
        given(order.getBelongsToField("technology")).willReturn(technology);
        given(product.getField("batch")).willReturn("test");
        given(dataDefinitionService.get("genealogies", "currentAttribute").find().withMaxResults(1).list().getEntities())
                .willReturn(getCurrentAttribute());

        given(technology.getField("shiftFeatureRequired")).willReturn(true);
        given(technology.getField("postFeatureRequired")).willReturn(true);
        given(technology.getField("otherFeatureRequired")).willReturn(true);

        doAnswer(new Answer<Object>() {

            @SuppressWarnings("unchecked")
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                ((List<Entity>) args[1]).add(createOperationComponent(false));
                ((List<Entity>) args[1]).add(createOperationComponent(false));
                ((List<Entity>) args[1]).add(createOperationComponent(false));
                ((List<Entity>) args[1]).add(createOperationComponent(false));
                return null;
            }
        }).when(genealogyService).addOperationsFromSubtechnologiesToList(any(EntityTree.class), anyListOf(Entity.class));

        given(dataDefinitionService.get("genealogies", "genealogy").save(any(Entity.class)).isValid()).willReturn(true);

        given(translationService.translate("genealogies.message.autoGenealogy.success", Locale.ENGLISH)).willReturn(
                "genealogies.message.autoGenealogy.success.pl");

        // when
        autoGenealogyService.autocompleteGenealogy(viewDefinitionState, state, new String[] { "false" });

        // then
        verify(state, times(2)).getFieldValue();

        verify(state).addMessage("genealogies.message.autoGenealogy.success.pl", MessageType.SUCCESS);
    }

    @Test
    public void shouldAutoCreateGenealogyWithLastUsedBatch() {
        // given
        ComponentState state = mock(ComponentState.class);
        given(state.getFieldValue()).willReturn(13L);
        given(state.getLocale()).willReturn(Locale.ENGLISH);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        Entity order = mock(Entity.class);
        given(dataDefinitionService.get("products", "order").get(13L)).willReturn(order);
        Entity product = mock(Entity.class);
        Entity technology = mock(Entity.class);
        given(order.getBelongsToField("product")).willReturn(product);
        given(order.getBelongsToField("technology")).willReturn(technology);
        given(product.getField("lastUsedBatch")).willReturn("test");
        given(dataDefinitionService.get("genealogies", "currentAttribute").find().withMaxResults(1).list().getEntities())
                .willReturn(getCurrentAttribute());

        given(technology.getField("shiftFeatureRequired")).willReturn(true);
        given(technology.getField("postFeatureRequired")).willReturn(true);
        given(technology.getField("otherFeatureRequired")).willReturn(true);

        doAnswer(new Answer<Object>() {

            @SuppressWarnings("unchecked")
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                ((List<Entity>) args[1]).add(createOperationComponent(false));
                ((List<Entity>) args[1]).add(createOperationComponent(false));
                ((List<Entity>) args[1]).add(createOperationComponent(false));
                ((List<Entity>) args[1]).add(createOperationComponent(false));
                return null;
            }
        }).when(genealogyService).addOperationsFromSubtechnologiesToList(any(EntityTree.class), anyListOf(Entity.class));

        given(dataDefinitionService.get("genealogies", "genealogy").save(any(Entity.class)).isValid()).willReturn(true);

        given(translationService.translate("genealogies.message.autoGenealogy.success", Locale.ENGLISH)).willReturn(
                "genealogies.message.autoGenealogy.success.pl");

        // when
        autoGenealogyService.autocompleteGenealogy(viewDefinitionState, state, new String[] { "true" });

        // then
        verify(state, times(2)).getFieldValue();

        verify(state).addMessage("genealogies.message.autoGenealogy.success.pl", MessageType.SUCCESS);
    }

    @Test
    public void shouldFailAutoCreateGenealogyWithLastUsedBatch() {
        // given
        ComponentState state = mock(ComponentState.class);
        given(state.getFieldValue()).willReturn(13L);
        given(state.getLocale()).willReturn(Locale.ENGLISH);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        Entity order = mock(Entity.class);
        given(dataDefinitionService.get("products", "order").get(13L)).willReturn(order);
        Entity product = mock(Entity.class);
        Entity technology = mock(Entity.class);
        given(order.getBelongsToField("product")).willReturn(product);
        given(order.getBelongsToField("technology")).willReturn(technology);
        given(product.getField("lastUsedBatch")).willReturn("test");
        given(dataDefinitionService.get("genealogies", "currentAttribute").find().withMaxResults(1).list().getEntities())
                .willReturn(new ArrayList<Entity>());
        given(technology.getField("shiftFeatureRequired")).willReturn(true);
        given(technology.getField("postFeatureRequired")).willReturn(true);
        given(technology.getField("otherFeatureRequired")).willReturn(true);

        doAnswer(new Answer<Object>() {

            @SuppressWarnings("unchecked")
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                ((List<Entity>) args[1]).add(createOperationComponent(true));
                ((List<Entity>) args[1]).add(createOperationComponent(true));
                ((List<Entity>) args[1]).add(createOperationComponent(true));
                ((List<Entity>) args[1]).add(createOperationComponent(true));
                return null;
            }
        }).when(genealogyService).addOperationsFromSubtechnologiesToList(any(EntityTree.class), anyListOf(Entity.class));

        given(translationService.translate("genealogies.message.autoGenealogy.missingShift", Locale.ENGLISH)).willReturn(
                "genealogies.message.autoGenealogy.missingShift.pl");

        given(translationService.translate("genealogies.message.autoGenealogy.missingOther", Locale.ENGLISH)).willReturn(
                "genealogies.message.autoGenealogy.missingOther.pl");

        given(translationService.translate("genealogies.message.autoGenealogy.missingPost", Locale.ENGLISH)).willReturn(
                "genealogies.message.autoGenealogy.missingPost.pl");

        given(translationService.translate("genealogies.message.autoGenealogy.missingBatch", Locale.ENGLISH)).willReturn(
                "genealogies.message.autoGenealogy.missingBatch.pl");

        mockStatic(TransactionAspectSupport.class);

        TransactionStatus transactionStatus = mock(TransactionStatus.class);

        given(TransactionAspectSupport.currentTransactionStatus()).willReturn(transactionStatus);

        // when
        autoGenealogyService.autocompleteGenealogy(viewDefinitionState, state, new String[] { "true" });

        // then
        verify(state, times(2)).getFieldValue();

        verify(state).addMessage("genealogies.message.autoGenealogy.missingShift.pl", MessageType.INFO, false);
        verify(state).addMessage("genealogies.message.autoGenealogy.missingOther.pl", MessageType.INFO, false);
        verify(state).addMessage("genealogies.message.autoGenealogy.missingPost.pl", MessageType.INFO, false);
        verify(state).addMessage(
                "genealogies.message.autoGenealogy.missingBatch.pl\nnumber1-name1; \nnumber3-name3; \nnumber4-name4; ",
                MessageType.INFO, false);
    }

    @Test
    public void shouldFailAutoCreateGenealogyWithLastUsedBatchOtherError() {
        // given
        ComponentState state = mock(ComponentState.class);
        given(state.getFieldValue()).willReturn(13L);
        given(state.getLocale()).willReturn(Locale.ENGLISH);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        Entity order = mock(Entity.class);
        given(dataDefinitionService.get("products", "order").get(13L)).willReturn(order);
        Entity product = mock(Entity.class);
        Entity technology = mock(Entity.class);
        given(order.getBelongsToField("product")).willReturn(product);
        given(order.getBelongsToField("technology")).willReturn(technology);
        given(product.getField("lastUsedBatch")).willReturn("test");
        given(technology.getField("shiftFeatureRequired")).willReturn(false);
        given(technology.getField("postFeatureRequired")).willReturn(false);
        given(technology.getField("otherFeatureRequired")).willReturn(false);

        given(translationService.translate("genealogies.message.autoGenealogy.failure", Locale.ENGLISH)).willReturn(
                "genealogies.message.autoGenealogy.failure.pl");

        given(dataDefinitionService.get("genealogies", "genealogy").save(any(Entity.class)).getGlobalErrors().isEmpty())
                .willReturn(true);

        mockStatic(TransactionAspectSupport.class);

        TransactionStatus transactionStatus = mock(TransactionStatus.class);

        given(TransactionAspectSupport.currentTransactionStatus()).willReturn(transactionStatus);

        // when
        autoGenealogyService.autocompleteGenealogy(viewDefinitionState, state, new String[] { "true" });

        // then
        verify(state, times(2)).getFieldValue();

        verify(state).addMessage("genealogies.message.autoGenealogy.failure.pl", MessageType.INFO);

    }

    @Test
    public void shouldFailAutoCreateGenealogyOnChangeOrderStatusIfNoRowIsSelected() throws Exception {
        // given
        ComponentState state = mock(ComponentState.class);
        given(state.getFieldValue()).willReturn(null);
        given(state.getLocale()).willReturn(Locale.ENGLISH);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(translationService.translate("core.grid.noRowSelectedError", Locale.ENGLISH)).willReturn(
                "core.grid.noRowSelectedError.pl");

        // when
        autoGenealogyService.generateGenalogyOnChangeOrderStatusForDone(viewDefinitionState, state, new String[] { "false" });

        // then
        verify(state).addMessage("core.grid.noRowSelectedError.pl", MessageType.FAILURE);
    }

    @Test
    public void shouldFailAutoCreateGenealogyOnChangeOrderStatusIfFormHasNoIdentifier() throws Exception {
        // given
        FormComponentState state = mock(FormComponentState.class);
        given(state.getFieldValue()).willReturn(null);
        given(state.getLocale()).willReturn(Locale.ENGLISH);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        given(translationService.translate("core.form.entityWithoutIdentifier", Locale.ENGLISH)).willReturn(
                "core.form.entityWithoutIdentifier.pl");

        // when
        autoGenealogyService.generateGenalogyOnChangeOrderStatusForDone(viewDefinitionState, state, new String[] { "false" });

        // then
        verify(state).addMessage("core.form.entityWithoutIdentifier.pl", MessageType.FAILURE);
    }

    @Test
    public void shouldFailAutoCreateGenealogyOnChangeOrderStatusIfOrderIsNull() {
        // given
        ComponentState state = mock(ComponentState.class);
        given(state.getFieldValue()).willReturn(13L);
        given(state.getLocale()).willReturn(Locale.ENGLISH);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);

        given(dataDefinitionService.get("products", "order").get(13L)).willReturn(null);

        given(translationService.translate("core.message.entityNotFound", Locale.ENGLISH)).willReturn(
                "core.message.entityNotFound.pl");
        // when
        autoGenealogyService.generateGenalogyOnChangeOrderStatusForDone(viewDefinitionState, state, new String[] { "false" });

        // then
        verify(state, times(2)).getFieldValue();
        verify(state).addMessage("core.message.entityNotFound.pl", MessageType.FAILURE);
    }

    @Test
    public void shouldAutoCreateGenealogyOnChangeOrderStatusWithActualBatch() {
        // given
        ComponentState state = mock(ComponentState.class);
        given(state.getFieldValue()).willReturn(13L);
        given(state.getLocale()).willReturn(Locale.ENGLISH);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);

        Entity order = mock(Entity.class);

        given(dataDefinitionService.get("products", "order").get(13L)).willReturn(order);

        given(dataDefinitionService.get("basic", "parameter").find().withMaxResults(1).list().getEntities()).willReturn(
                getParameter("01active"));

        Entity product = mock(Entity.class);
        Entity technology = mock(Entity.class);
        given(order.getBelongsToField("product")).willReturn(product);
        given(order.getBelongsToField("technology")).willReturn(technology);
        given(product.getField("batch")).willReturn("test");

        given(technology.getField("shiftFeatureRequired")).willReturn(false);
        given(technology.getField("postFeatureRequired")).willReturn(false);
        given(technology.getField("otherFeatureRequired")).willReturn(false);

        doAnswer(new Answer<Object>() {

            @SuppressWarnings("unchecked")
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                ((List<Entity>) args[1]).add(createOperationComponent(false));
                ((List<Entity>) args[1]).add(createOperationComponent(false));
                ((List<Entity>) args[1]).add(createOperationComponent(false));
                ((List<Entity>) args[1]).add(createOperationComponent(false));
                return null;
            }
        }).when(genealogyService).addOperationsFromSubtechnologiesToList(any(EntityTree.class), anyListOf(Entity.class));

        given(dataDefinitionService.get("genealogies", "genealogy").save(any(Entity.class)).isValid()).willReturn(true);

        given(translationService.translate("genealogies.message.autoGenealogy.success", Locale.ENGLISH)).willReturn(
                "genealogies.message.autoGenealogy.success.pl");

        // when
        autoGenealogyService.generateGenalogyOnChangeOrderStatusForDone(viewDefinitionState, state, new String[] { "false" });

        // then
        verify(state, times(2)).getFieldValue();

        verify(state).addMessage("genealogies.message.autoGenealogy.success.pl", MessageType.SUCCESS);

    }

    @Test
    public void shouldAutoCreateGenealogyOnChangeOrderStatusWithLastUsedBatch() {
        // given
        ComponentState state = mock(ComponentState.class);
        given(state.getFieldValue()).willReturn(13L);
        given(state.getLocale()).willReturn(Locale.ENGLISH);
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);

        Entity order = mock(Entity.class);

        given(dataDefinitionService.get("products", "order").get(13L)).willReturn(order);

        given(dataDefinitionService.get("basic", "parameter").find().withMaxResults(1).list().getEntities()).willReturn(
                getParameter("02lastUsed"));

        Entity product = mock(Entity.class);
        Entity technology = mock(Entity.class);
        given(order.getBelongsToField("product")).willReturn(product);
        given(order.getBelongsToField("technology")).willReturn(technology);
        given(product.getField("lastUsedBatch")).willReturn("test");

        given(technology.getField("shiftFeatureRequired")).willReturn(false);
        given(technology.getField("postFeatureRequired")).willReturn(false);
        given(technology.getField("otherFeatureRequired")).willReturn(false);

        doAnswer(new Answer<Object>() {

            @SuppressWarnings("unchecked")
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                ((List<Entity>) args[1]).add(createOperationComponent(false));
                ((List<Entity>) args[1]).add(createOperationComponent(false));
                ((List<Entity>) args[1]).add(createOperationComponent(false));
                ((List<Entity>) args[1]).add(createOperationComponent(false));
                return null;
            }
        }).when(genealogyService).addOperationsFromSubtechnologiesToList(any(EntityTree.class), anyListOf(Entity.class));

        given(dataDefinitionService.get("genealogies", "genealogy").save(any(Entity.class)).isValid()).willReturn(true);

        given(translationService.translate("genealogies.message.autoGenealogy.success", Locale.ENGLISH)).willReturn(
                "genealogies.message.autoGenealogy.success.pl");

        // when
        autoGenealogyService.generateGenalogyOnChangeOrderStatusForDone(viewDefinitionState, state, new String[] { "false" });

        // then
        verify(state, times(2)).getFieldValue();

        verify(state).addMessage("genealogies.message.autoGenealogy.success.pl", MessageType.SUCCESS);

    }

    @Test
    public void shouldFillLastUsedShiftFeature() {
        // given
        Entity entity = new DefaultEntity("test", "entity");
        entity.setField("value", "newTest");

        DataDefinition dataDefinition = mock(DataDefinition.class);
        Entity feature = mock(Entity.class);
        List<Entity> featureEntities = new ArrayList<Entity>();
        featureEntities.add(feature);

        given(dataDefinitionService.get("genealogies", "currentAttribute").find().withMaxResults(1).list().getEntities())
                .willReturn(featureEntities);
        given(securityService.getCurrentUserName()).willReturn("newTest");

        // when
        autoGenealogyService.fillLastUsedShiftFeature(dataDefinition, entity);
        // then
        verify(dataDefinitionService.get("genealogies", "currentAttribute").find().withMaxResults(1).list().getEntities().get(0))
                .setField(anyString(), anyString());
        verify(dataDefinitionService.get("genealogies", "currentAttribute")).save(any(Entity.class));
        assertNotNull(entity.getField("date"));
        assertNotNull(entity.getField("worker"));
    }

    @Test
    public void shouldFillLastUsedOtherFeature() {
        // given
        Entity entity = new DefaultEntity("test", "entity");
        entity.setField("value", "newTest");

        DataDefinition dataDefinition = mock(DataDefinition.class);
        Entity feature = mock(Entity.class);
        List<Entity> featureEntities = new ArrayList<Entity>();
        featureEntities.add(feature);

        given(dataDefinitionService.get("genealogies", "currentAttribute").find().withMaxResults(1).list().getEntities())
                .willReturn(featureEntities);
        given(securityService.getCurrentUserName()).willReturn("newTest");

        // when
        autoGenealogyService.fillLastUsedOtherFeature(dataDefinition, entity);
        // then
        verify(dataDefinitionService.get("genealogies", "currentAttribute").find().withMaxResults(1).list().getEntities().get(0))
                .setField(anyString(), anyString());
        verify(dataDefinitionService.get("genealogies", "currentAttribute")).save(any(Entity.class));
        assertNotNull(entity.getField("date"));
        assertNotNull(entity.getField("worker"));
    }

    @Test
    public void shouldFillLastUsedPostFeature() {
        // given
        Entity entity = new DefaultEntity("test", "entity");
        entity.setField("value", "newTest");

        DataDefinition dataDefinition = mock(DataDefinition.class);
        Entity feature = mock(Entity.class);
        List<Entity> featureEntities = new ArrayList<Entity>();
        featureEntities.add(feature);

        given(dataDefinitionService.get("genealogies", "currentAttribute").find().withMaxResults(1).list().getEntities())
                .willReturn(featureEntities);
        given(securityService.getCurrentUserName()).willReturn("newTest");

        // when
        autoGenealogyService.fillLastUsedPostFeature(dataDefinition, entity);
        // then
        verify(dataDefinitionService.get("genealogies", "currentAttribute").find().withMaxResults(1).list().getEntities().get(0))
                .setField(anyString(), anyString());
        verify(dataDefinitionService.get("genealogies", "currentAttribute")).save(any(Entity.class));
        assertNotNull(entity.getField("date"));
        assertNotNull(entity.getField("worker"));
    }

    @Test
    public void shouldFillLastUsedBatchForProduct() {
        // given
        Entity entity = mock(Entity.class, RETURNS_DEEP_STUBS);
        given(entity.getField("date")).willReturn(null);
        given(entity.getField("worker")).willReturn(null);

        DataDefinition dataDefinition = mock(DataDefinition.class);

        given(securityService.getCurrentUserName()).willReturn("newTest");

        // when
        autoGenealogyService.fillLastUsedBatchForProduct(dataDefinition, entity);

        // then
        verify(entity, times(2)).setField(anyString(), any());
        verify(dataDefinitionService.get("products", "product").get(anyLong())).setField(anyString(), anyString());
        verify(dataDefinitionService.get("products", "product")).save(any(Entity.class));
    }

    @Test
    public void shouldFillLastUsedBatchForGenealogy() {
        // given
        Entity entity = mock(Entity.class, RETURNS_DEEP_STUBS);
        given(entity.getField("date")).willReturn(null);
        given(entity.getField("worker")).willReturn(null);

        DataDefinition dataDefinition = mock(DataDefinition.class);

        given(securityService.getCurrentUserName()).willReturn("newTest");

        // when
        autoGenealogyService.fillLastUsedBatchForGenealogy(dataDefinition, entity);

        // then
        verify(entity, times(2)).setField(anyString(), any());
        verify(dataDefinitionService.get("products", "product").get(anyLong())).setField(anyString(), anyString());
        verify(dataDefinitionService.get("products", "product")).save(any(Entity.class));
    }

    private Entity createOperationComponent(final boolean withoutBatch) {
        Entity operationComponent = mock(Entity.class);
        List<Entity> productsEntities = new ArrayList<Entity>();
        productsEntities.add(createOperationProductInComponent(1L, true, withoutBatch));
        productsEntities.add(createOperationProductInComponent(2L, false, withoutBatch));
        productsEntities.add(createOperationProductInComponent(3L, true, withoutBatch));
        productsEntities.add(createOperationProductInComponent(4L, true, withoutBatch));
        DataDefinition listDataDefinition = mock(DataDefinition.class, RETURNS_DEEP_STUBS);
        given(listDataDefinition.find().restrictedWith(any(Restriction.class)).list().getEntities()).willReturn(productsEntities);

        EntityList operationProductInComponents = new EntityList(listDataDefinition, "joinField", 1L);

        given(operationComponent.getHasManyField("operationProductInComponents")).willReturn(operationProductInComponents);
        return operationComponent;
    }

    private Entity createOperationProductInComponent(final Long id, final boolean batchRequired, final boolean withoutBatch) {
        Entity operationProductInComponent = new DefaultEntity("products", "operationProductInComponent", id);
        operationProductInComponent.setField("batchRequired", batchRequired);
        Entity product = new DefaultEntity("products", "product", id);
        product.setField("name", "name" + id);
        product.setField("number", "number" + id);
        if (!withoutBatch) {
            product.setField("batch", "batch" + id);
            product.setField("lastUsedBatch", "batch" + id);
        }
        operationProductInComponent.setField("product", product);
        return operationProductInComponent;
    }

    private List<Entity> getCurrentAttribute() {
        List<Entity> list = new ArrayList<Entity>();
        Entity currentAttribute = new DefaultEntity("genealogies", "currentAttribute");
        currentAttribute.setField("shift", "test");
        currentAttribute.setField("post", "test");
        currentAttribute.setField("other", "test");
        currentAttribute.setField("lastUsedShift", "test");
        currentAttribute.setField("lastUsedPost", "test");
        currentAttribute.setField("lastUsedOther", "test");
        list.add(currentAttribute);
        return list;
    }

    private List<Entity> getParameter(final String value) {
        List<Entity> list = new ArrayList<Entity>();
        Entity currentAttribute = new DefaultEntity("basic", "parameter");
        currentAttribute.setField("batchForDoneOrder", value);
        list.add(currentAttribute);
        return list;
    }
}
